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
import org.apache.commons.io.FileUtils;
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
import novemberdobby.teamcity.imageComp.common.DiffResult;

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
            Long referenceBuildID = -1L;
            String referenceBuildNumber = "";
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
                    referenceBuildID = Long.parseLong((String)xpath.evaluate("/builds/build/@id", buildsDoc, XPathConstants.STRING));
                    referenceBuildNumber = (String)xpath.evaluate("/builds/build/@number", buildsDoc, XPathConstants.STRING);
                } catch (Exception e) {
                    log.error(e.toString());
                    log.error("This may mean that no suitable build is available to compare against");
                }
                
                File tempDir = new File(build.getBuildTempDirectory(), "image_comp_sources");
                tempDir.mkdir();

                if(referenceBuildID != -1) {
                    log.message(String.format("Reference build is %s/viewLog.html?buildId=%s", serverUrl, referenceBuildID));

                    List<String> paths = Arrays.asList(pathsParam.split("[\n\r]"));
                    Map<String, File> storedItems = new HashMap<>();

                    int idx = -1;
                    for (String artifact : paths) {
                        idx++;
                        String extension = FilenameUtils.getExtension(artifact);
                        if(extension == null || extension.length() == 0) {
                            log.error(String.format("Missing extension for artifact: %s", artifact)); //we need this or the tools won't know what to do!
                            continue;
                        }

                        File target = new File(tempDir, String.format("%s.%s", idx, extension));
                        log.message(String.format("Downloading %s to %s", artifact, target.getAbsolutePath()));

                        //note: this creates an artifact dependency, the server matches our credentials with the source build to create the link
                        String downloadUrl = String.format("%s/httpAuth/app/rest/builds/%s/artifacts/content/%s", serverUrl, referenceBuildID, artifact);
                        try {
                            URLConnection connection = Util.webRequest(downloadUrl, build.getAccessUser(), build.getAccessCode());
                            Util.downloadFile(connection, target);
                        } catch (Exception e) {
                            log.error("Download failed:");
                            log.error(e.toString());
                            continue;
                        }

                        storedItems.put(artifact, target);
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
                            //we can potentially modify images in the cache, so copy them elsewhere first (preserve ext for magick)
                            File tempCacheCopy = new File(tempDir, String.format("_cache_temp.%s", FilenameUtils.getExtension(cachedFile.getAbsolutePath())));
                            try {
                                FileUtils.copyFile(cachedFile, tempCacheCopy, false);
                            } catch (Exception e) {
                                log.error(String.format("Couldn't create temp file from cache item: %s (%s)", cachedFile.getAbsolutePath(), tempCacheCopy.getAbsolutePath()));
                                continue;
                            }

                            compare(build, params, stored.getKey(), stored.getValue(), referenceBuildNumber, tempCacheCopy);

                            if(build.getInterruptReason() != null) {
                                log.warning("Stopping comparisons due to interrupted build");
                                break;
                            }
                        } else {
                            log.error(String.format("Couldn't find file in cache: %s", cachedFile.getAbsolutePath()));
                        }
                    }

                } else {
                    log.error("Skipping downloads due to errors");
                }
            }

            if(build.getInterruptReason() != null) {
                break;
            }
        }

        log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
    }

    boolean compare(AgentRunningBuild build, Map<String, String> params, String artifactName, File referenceImage, String referenceVersion, File newImage) {
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

            //TODO: support tolerance aka -fuzz and make a note of what the value was (may as well record source build too if we're gonna do that)
            DiffResult diff = imageMagickDiff(magick, metric, referenceImage, newImage, tempDiffImage);

            if(diff.Success) {
                log.message(String.format("Result for %s: %s", metric.toUpperCase(), diff.DifferenceAmount));
                if(first) {
                    log.message(String.format("##teamcity[publishArtifacts '%s => %s']", tempDiffImage.getAbsolutePath(), Constants.ARTIFACTS_RESULT_PATH));
                }
                
                log.message(String.format("##teamcity[buildStatisticValue key='ic_%s_%s' value='%.6f']", artifactName, metric, diff.DifferenceAmount));

            } else {
                log.error(String.format("Result for %s: %s", metric.toUpperCase(), diff.StandardOut));
            }

            if(build.getInterruptReason() != null) {
                return false;
            }
        }

        if("true".equals(params.get(Constants.FEATURE_SETTING_GENERATE_ANIMATED))) {
            String artifactNameAnimated = String.format("%s_animated.webp", FilenameUtils.removeExtension(artifactName));
            File tempAnimatedImage = new File(diffImagesTemp, artifactNameAnimated);

            File[] images = new File[] { referenceImage, newImage };
            String[] annotations = new String[] { String.format("Baseline: #%s", build.getBuildNumber()), String.format("This build: #%s", referenceVersion) };

            if(imageMagickAnimate(magick, images, annotations, tempAnimatedImage)) {
                log.message(String.format("##teamcity[publishArtifacts '%s => %s']", tempAnimatedImage.getAbsolutePath(), Constants.ARTIFACTS_RESULT_PATH));
            } else {
                log.error(String.format("Webp creation failed for %s", tempAnimatedImage.getAbsolutePath()));
            }
        }

        //TODO: unlikely to work well with similarly named files in different places in artifacts, support mirroring folder structure
        //TODO: meaningful return
        return true;
        //TODO: non-windows agent support
    }

    DiffResult imageMagickDiff(File toolPath, String metric, File referenceImage, File newImage, File createDifferenceImage) {
        
        CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
        cmdLine.addArguments("compare -quality 100 -metric");
        cmdLine.addArgument(metric);
        cmdLine.addArguments("-compose src");
        cmdLine.addArgument(referenceImage.getAbsolutePath());
        cmdLine.addArgument(newImage.getAbsolutePath());
        cmdLine.addArgument(createDifferenceImage.getAbsolutePath());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outStream));

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

    boolean imageMagickAnimate(File toolPath, File[] images, String[] annotations, File createImage) {
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outStream));
        executor.setWatchdog(new ExecuteWatchdog(60000));

        //annotate each image
        for (int i = 0; i < images.length; i++) {

            CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
            cmdLine.addArgument("mogrify"); //modify in-place
            cmdLine.addArguments("-quality 100 -background #888f -gravity north -splice 0x43 -gravity northwest -pointsize 20 -draw");
            cmdLine.addArgument(String.format("text 10,10 '%s'", annotations[i]));
            cmdLine.addArgument(images[i].getAbsolutePath());

            try {
                executor.execute(cmdLine);
            } catch (Exception e) {
                return false;
            }

            outStream.reset();
        }

        //composite into an animated webp image
        CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
        cmdLine.addArguments("convert -quality 100 -define webp:lossless=true -delay 100 -loop 0");
        for (File image : images) {
            cmdLine.addArgument(image.getAbsolutePath());
        }
        cmdLine.addArgument(createImage.getAbsolutePath());

        try {
            executor.execute(cmdLine);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}