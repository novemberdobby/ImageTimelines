package novemberdobby.teamcity.imageComp.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.util.EventDispatcher;

import novemberdobby.teamcity.imageComp.common.Constants;
import novemberdobby.teamcity.imageComp.common.Util;

public class Processor extends AgentLifeCycleAdapter {
    
    public Processor(EventDispatcher<AgentLifeCycleListener> events) {
        events.addListener(this);
    }
    
    @Override
    public void afterAtrifactsPublished(AgentRunningBuild build, BuildFinishedStatus status) {
        BuildProgressLogger log = build.getBuildLogger();

        String blockMsg = String.format("Image Comparison: processing artifacts");
        log.activityStarted(blockMsg, "CUSTOM_IMAGE_COMP");
        
        Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID);
        for (AgentBuildFeature feature : features) {
            Map<String, String> params = feature.getParameters();
            String pathsParam = params.get(Constants.FEATURE_SETTING_ARTIFACTS);

            //download artifacts from an earlier build to agent temp
            Long sourceBuildID = -1L;
            if(pathsParam != null) {
                String serverUrl = build.getAgentConfiguration().getServerUrl();
                String buildIntId = build.getBuildTypeId(); //resist external ID changes
                String restrict = "";

                if("tagged".equals(params.get(Constants.FEATURE_SETTING_COMPARE_TYPE))) {
                    restrict = String.format(",tag:%s", params.get(Constants.FEATURE_SETTING_TAG));
                }

                String infoUrl = String.format("%s/httpAuth/app/rest/builds?locator=buildType(internalId:%s),count:1%s", serverUrl, buildIntId, restrict);
                
                try {
                    Document buildsDoc = Util.getRESTdocument(infoUrl, build.getAccessUser(), build.getAccessCode());
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    sourceBuildID = Long.parseLong((String)xpath.evaluate("/builds/build/@id", buildsDoc, XPathConstants.STRING));
                } catch (Exception e) {
                    log.error(e.toString());
                    log.error("This may mean that no suitable build is available to compare against");
                }
                
                if(sourceBuildID != -1) {
                    log.message(String.format("Source build ID is %s", sourceBuildID));

                    File tempDir = new File(build.getBuildTempDirectory(), "image_comp_sources");
                    tempDir.mkdir();

                    List<String> paths = Arrays.asList(pathsParam.split("[\n\r]"));
                    Map<String, File> storedItems = new HashMap<>();

                    int idx = -1;
                    for (String path : paths) {
                        idx++;
                        String extension = FilenameUtils.getExtension(path);
                        if(extension == null || extension.length() == 0) {
                            log.error(String.format("Missing extension for artifact: %s", path)); //we need this or the tools won't know what to do!
                            continue;
                        }

                        File target = new File(tempDir, String.format("%s.%s", idx, extension));
                        log.message(String.format("Downloading %s to %s", path, target.getAbsolutePath()));

                        //note: this creates an artifact dependency, the server matches our credentials with the source build to create the link
                        String downloadUrl = String.format("%s/httpAuth/app/rest/builds/%s/artifacts/content/%s", serverUrl, sourceBuildID, path);
                        try {
                            URLConnection connection = Util.webRequest(downloadUrl, build.getAccessUser(), build.getAccessCode());
                            Util.downloadFile(connection, target);
                        } catch (Exception e) {
                            log.error("Download failed:");
                            log.error(e.toString());
                            continue;
                        }

                        storedItems.put(path, target);
                    }

                    //now we've got as many as we can, iterate and do the comparisons
                    //first get the files we just published as artifacts. find them in the local artifact cache - if that fails, redownload from server (we can't find out their pre-publish workspace paths)
                    File cache = build.getAgentConfiguration().getCacheDirectory(".artifacts_cache");
                    URL serverUrlObj = null;
                    try {
                        serverUrlObj = new URL(serverUrl);
                    } catch (MalformedURLException e) {
                        log.error("Failed to create URL object from server url (this really shouldn't happen)");
                        return;
                    }
                    
                    //e.g. C:\TeamCity\buildAgent\system\.artifacts_cache\localhost_80\httpAuth\repository\download\ConfigA\6466.tcbuildid
                    File buildCachePath = new File(String.format("%s\\%s_%s\\httpAuth\\repository\\download\\%s\\%s.tcbuildid",
                        cache, serverUrlObj.getHost(), serverUrlObj.getPort(), build.getBuildTypeExternalId(), build.getBuildId()));
                    
                    if(!buildCachePath.exists()) {
                        log.error(String.format("Couldn't find cache path: %s", buildCachePath.getAbsolutePath())); //TODO: warning & fall back to server download
                        return;
                    }
                    
                    log.message(String.format("Found artifacts cache: %s", buildCachePath.getAbsolutePath()));
                    for (Entry<String, File> stored : storedItems.entrySet()) {
                        File cachedFile = new File(buildCachePath, stored.getKey());
                        if(cachedFile.exists()) {
                            compare(build, params, stored.getKey(), stored.getValue(), cachedFile);
                        } else {
                            log.message(String.format("Couldn't find file in cache: %s", cachedFile.getAbsolutePath()));
                        }
                    }

                } else {
                    log.error("Skipping downloads due to errors");
                }
            }
        }

        log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
    }

    boolean compare(AgentRunningBuild build, Map<String, String> params, String artifactName, File referenceImage, File newImage) {
        BuildProgressLogger log = build.getBuildLogger();
        
        ValueResolver resolvedParams = build.getSharedParametersResolver();
        ProcessingResult resolveResult = resolvedParams.resolve("%" + Constants.TOOL_IM_PATH_PARAM + "%");
        if(!resolveResult.isFullyResolved()) {
            return false;
        }

        File magick = new File(resolveResult.getResult(), "magick.exe");

        //make a new folder to ensure we don't step on any inputs
        File diffImagesTemp = new File(build.getBuildTempDirectory(), "image_comp_diffs");
        diffImagesTemp.mkdir();

        List<String> metrics = Util.getCompareMetrics(params);
        for (String metric : metrics) {
            //each time we invoke compare, we *have* to produce a diff image. these are identical regardless of which metric is used.
            //publish the first as an artifact, and give the rest another name so publishing doesn't break when the image is overwritten
            boolean first = metric == metrics.get(0);
            String artPrefix = first ? "" : "_";
            String artifactNameDiff = String.format("%s_diff.%s", FilenameUtils.removeExtension(artifactName), FilenameUtils.getExtension(artifactName));
            File tempDiffImage = new File(diffImagesTemp, artPrefix + artifactNameDiff);

            DiffResult diff = imageMagickDiff(magick, metric, referenceImage, newImage, tempDiffImage);

            if(diff.Success) {
                log.message(String.format("Result for %s: %s", metric.toUpperCase(), diff.DifferenceAmount));
                if(first) {
                    log.message(String.format("##teamcity[publishArtifacts '%s => image_comparisons']", tempDiffImage.getAbsolutePath()));
                }
            } else {
                log.error(String.format("Result for %s: %s", metric.toUpperCase(), diff.StandardOut));
            }
        }

        //TODO: optional
        //TODO: expand canvas and write 'before/after' at the top
        String artifactNameFlicker = String.format("%s_flicker.gif", FilenameUtils.removeExtension(artifactName));
        File tempFlickerImage = new File(diffImagesTemp, artifactNameFlicker);

        if(imageMagickFlicker(magick, referenceImage, newImage, tempFlickerImage)) {
            log.message(String.format("##teamcity[publishArtifacts '%s => image_comparisons']", tempFlickerImage.getAbsolutePath()));
        }

        //TODO: unlikely to work well with similarly named files in different places in artifacts
        //TODO: meaningful return
        return true;
        //TODO: TC statistic
        //TODO: beyond compare diff report option - can an agent requirement be optional based on whether it's enabled? won't work with the hidden prop hack
        //TODO: non-windows agent support
        //TODO: need to mark a build with the baseline it was compared against so there's no confusion if options change
    }

    //TODO: put this in common module so server can run it for arbitrary builds
    DiffResult imageMagickDiff(File toolPath, String metric, File referenceImage, File newImage, File createDifferenceImage) {
        
        DefaultExecutor executor = new DefaultExecutor();
        
        CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
        cmdLine.addArgument("compare");
        cmdLine.addArgument("-metric");
        cmdLine.addArgument(metric);
        cmdLine.addArgument("-compose");
        cmdLine.addArgument("src");
        cmdLine.addArgument(referenceImage.getAbsolutePath());
        cmdLine.addArgument(newImage.getAbsolutePath());
        cmdLine.addArgument(createDifferenceImage.getAbsolutePath());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outStream);
        executor.setStreamHandler(streamHandler);

        //we don't actually care about the exit code unless it's 2, in which case something has genuinely gone wrong
        executor.setExitValues(new int[] { 0, 1 });

        executor.setWatchdog(new ExecuteWatchdog(20000)); //this should be fairly quick to run
        try {
            executor.execute(cmdLine);
            return new DiffResult(false, outStream.toString());
        } catch (Exception e) {
            return new DiffResult(true, String.format("Exception when comparing:\r\n%s\r\nOutput: %s", e.toString(), outStream));
        }
    }

    boolean imageMagickFlicker(File toolPath, File referenceImage, File newImage, File createFlickerImage) {
        
        DefaultExecutor executor = new DefaultExecutor();

        CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
        cmdLine.addArgument("convert");
        cmdLine.addArgument("-delay");
        cmdLine.addArgument("50");
        cmdLine.addArgument(referenceImage.getAbsolutePath());
        cmdLine.addArgument(newImage.getAbsolutePath());
        cmdLine.addArgument("-loop");
        cmdLine.addArgument("0");
        cmdLine.addArgument(createFlickerImage.getAbsolutePath());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outStream);
        executor.setStreamHandler(streamHandler);
        executor.setWatchdog(new ExecuteWatchdog(20000));
        try {
            executor.execute(cmdLine);
            return true;
        } catch (Exception e) {
            return false; //TODO better feedback
        }
    }

    static class DiffResult {
        public boolean Success;
        public String StandardOut;
        public Double DifferenceAmount;

        public DiffResult(Boolean threwException, String output) {
            StandardOut = output;

            //IM can return non-zero codes for some comparisons, e.g. when the images are identical...
            //so fudge the "result" and just try to send a number back
            if(!threwException) {

                //TODO: is this a sensible result?
                if("1.#INF".equals(output)) {
                    DifferenceAmount = 0D;
                    Success = true;
                }
                else {
                    //a bunch of metrics show additional breakdowns e.g. "1 (0.5, 0.5)", so just send the first number back
                    int firstSpace = output.indexOf(" ");
                    if(firstSpace != -1) {
                        output = output.substring(0, firstSpace);
                    }

                    try {
                        DifferenceAmount = Double.parseDouble(output);
                        Success = true;
                    } catch (NumberFormatException e) { }
                }
            }
        }
    }
}