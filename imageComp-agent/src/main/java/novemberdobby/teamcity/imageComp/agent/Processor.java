package novemberdobby.teamcity.imageComp.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.util.Pair;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentSystemInfo;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.util.EventDispatcher;

import novemberdobby.teamcity.imageComp.common.Constants;
import novemberdobby.teamcity.imageComp.common.Util;
import novemberdobby.teamcity.imageComp.common.DiffResult;

public class Processor extends AgentLifeCycleAdapter {
    
    PluginDescriptor m_descriptor;

    public Processor(EventDispatcher<AgentLifeCycleListener> events, PluginDescriptor descriptor) {
        m_descriptor = descriptor;
        events.addListener(this);
    }
    
    @Override
    public void afterAtrifactsPublished(AgentRunningBuild build, BuildFinishedStatus status) {
        BuildProgressLogger log = build.getBuildLogger();

        String blockMsg = "Image Comparison";
        log.activityStarted(blockMsg, "CUSTOM_IMAGE_COMP");
        
        try {
            Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID);
            for (AgentBuildFeature feature : features) {
                Map<String, String> params = feature.getParameters();
                String pathsParam = params.get(Constants.FEATURE_SETTING_ARTIFACTS);
                boolean problemOnError = "true".equals(params.get(Constants.FEATURE_SETTING_FAIL_ON_ERROR));

                //TODO: 'set new baseline' button on specific build
                //TODO: generate thumbnails & show on page?
                //TODO: retry all 3 server requests X times to handle downtime. internal prop with retry count?

                //download artifacts from both builds to agent temp
                Long refBuildID = -1L;
                String refBuildNumber = "";
                if(pathsParam != null) {
                    String serverUrl = build.getAgentConfiguration().getServerUrl();

                    //TODO: escape tag
                    String infoUrl = String.format("%s%s?mode=process&buildTypeId=%s&%s=%s&tag=%s", serverUrl, Constants.FEATURE_REFERENCE_BUILD_URL,
                        build.getBuildTypeExternalId(), Constants.FEATURE_SETTING_COMPARE_TYPE, params.get(Constants.FEATURE_SETTING_COMPARE_TYPE), params.get(Constants.FEATURE_SETTING_TAG));

                    try {
                        List<String> resultStr = Util.webRequestLines(infoUrl, build.getAccessUser(), build.getAccessCode());
                        if(resultStr.size() == 1) {
                            String result = resultStr.get(0);
                            int comma = result.indexOf(',');
                            refBuildID = Long.parseLong(result.substring(0, comma));
                            refBuildNumber = result.substring(comma + 1);
                        }
                        
                    } catch (Throwable e) {
                        logError(log, problemOnError, "Couldn't find a suitable build to compare images against");
                        log.error(e.toString());
                        for (StackTraceElement st : e.getStackTrace()) {
                            log.error(st.toString());
                        }
                    }
                    
                    File tempDir = new File(build.getBuildTempDirectory(), "image_comp_sources");
                    tempDir.mkdir();

                    if(refBuildID != -1) {
                        log.message(String.format("Reference build is %s/viewLog.html?buildId=%s", serverUrl, refBuildID));

                        List<String> paths = Arrays.asList(pathsParam.split("[\n\r]"));
                        Map<String, Pair<File, File>> storedItems = new HashMap<>();

                        String downloadBlockMsg = "Downloading images";
                        log.activityStarted(downloadBlockMsg, "CUSTOM_IMAGE_COMP");

                        try {
                            int idx = -1;
                            for (String artifact : paths) {
                                idx++;
                                String extension = FilenameUtils.getExtension(artifact);
                                if(extension == null || extension.length() == 0) {
                                    logError(log, problemOnError, String.format("Missing extension for image artifact: %s", artifact)); //we need this or the tool won't know what to do!
                                    continue;
                                }

                                String tempFormat = String.format("%s.%s", idx, extension);
                                File refTarget = new File(tempDir, "ref_" + tempFormat);
                                File newTarget = new File(tempDir, "new_" + tempFormat);

                                //note: this creates an artifact dependency, the server matches our credentials with the source build to create the link
                                String refDownloadUrl = String.format("%s/httpAuth/app/rest/builds/%s/artifacts/content/%s", serverUrl, refBuildID, artifact);
                                String newDownloadUrl = String.format("%s/httpAuth/app/rest/builds/%s/artifacts/content/%s", serverUrl, build.getBuildId(), artifact);

                                log.message(String.format("%s to %s", refDownloadUrl, refTarget.getAbsolutePath()));
                                log.message(String.format("%s to %s", newDownloadUrl, newTarget.getAbsolutePath()));

                                try {
                                    URLConnection refConn = Util.webRequest(refDownloadUrl, build.getAccessUser(), build.getAccessCode());
                                    Util.downloadFile(refConn, refTarget);
                                    
                                    URLConnection newConn = Util.webRequest(newDownloadUrl, build.getAccessUser(), build.getAccessCode());
                                    Util.downloadFile(newConn, newTarget);
                                } catch (Throwable e) {
                                    logError(log, problemOnError, "Image artifact download failed");
                                    log.error(e.toString());
                                    continue;
                                }

                                storedItems.put(artifact, new Pair<>(refTarget, newTarget));
                            }
                                
                        } finally {
                            log.activityFinished(downloadBlockMsg, "CUSTOM_IMAGE_COMP");
                        }

                        //now we've got as many as we can, iterate and do the comparisons
                        for (Entry<String, Pair<File, File>> stored : storedItems.entrySet()) {

                            String artifactName = stored.getKey();
                            File refImage = stored.getValue().first;
                            File newImage = stored.getValue().second;

                            if(!compare(build, problemOnError, params, artifactName, refImage, refBuildNumber, newImage)) {
                                logError(log, problemOnError, "Image comparison failed for " + artifactName);
                            }

                            if(build.getInterruptReason() != null) {
                                log.warning("Stopping comparisons due to interrupted build");
                                break;
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
        } finally {
            log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
        }
    }

    void logError(BuildProgressLogger log, boolean problemOnError, String error) {
        if(problemOnError) {
            String identity = error.length() > 60 ? error.substring(0, 60) : error;
            BuildProblemData buildProblem = BuildProblemData.createBuildProblem(identity, BuildProblemData.TC_USER_PROVIDED_TYPE, error);
            log.logBuildProblem(buildProblem);
        } else {
            log.error(error);
        }
    }

    boolean compare(AgentRunningBuild build, boolean problemOnError, Map<String, String> params, String artifactName, File referenceImage, String referenceVersion, File newImage) {
        BuildProgressLogger log = build.getBuildLogger();
        
        String blockMsg = String.format("Comparing '%s'", artifactName);
        log.activityStarted(blockMsg, "CUSTOM_IMAGE_COMP");

        try {
            File magick = null;
            BuildAgentSystemInfo agentInfo = build.getAgentConfiguration().getSystemInfo();
            if(agentInfo.isWindows()) {
                magick = new File(m_descriptor.getPluginRoot(), "ImageMagick\\windows\\magick.exe");
            }
            else if(agentInfo.isUnix()) {
                magick = new File(m_descriptor.getPluginRoot(), "ImageMagick/unix/magick");
            }
            else {
                log.error("Unsupported OS");
                return false;
            }

            //make a new folder to ensure we don't step on any inputs
            File diffImagesTemp = new File(build.getBuildTempDirectory(), "image_comp_diffs");
            diffImagesTemp.mkdir();
            String publishToFolder = "";
            
            List<String> metrics = Util.getCompareMetrics(params);
            for (String metric : metrics) {
                //each time we invoke compare, we *have* to produce a diff image. these are identical regardless of which metric is used.
                //publish the first as an artifact, and give the rest another name so publishing doesn't break when the image is overwritten
                boolean first = metric == metrics.get(0);
                String artPrefix = first ? "" : "_";
                String artifactNameDiff = String.format("%s_diff.%s", FilenameUtils.removeExtension(artifactName), FilenameUtils.getExtension(artifactName));

                //don't try to write paths with ! in
                File tempDiffImage = removeArchive(new File(diffImagesTemp, artPrefix + artifactNameDiff));

                //TODO: support tolerance aka -fuzz and make a note of what the value was (may as well record source build too if we're gonna do that, then remove ajax rq or use as a fallback)
                //TODO: may as well publish another file to artifacts with info, can we use the same file for all this stuff? would need repeated publishes (risky)

                //create folder for diff image
                File tempParent = tempDiffImage.getParentFile();
                if(!tempParent.exists()) {
                    tempParent.mkdirs();
                }

                DiffResult diff = imageMagickDiff(magick, metric, referenceImage, newImage, tempDiffImage);

                if(diff.Success) {
                    if(first) {
                        //remove temp storage prefix to get destination
                        String baseFolder = diffImagesTemp.getAbsolutePath();
                        String fullPath = tempParent.getAbsolutePath();
                        if(fullPath.length() > baseFolder.length()) { //let's hope...
                            publishToFolder = fullPath.substring(baseFolder.length());
                        }

                        log.message(String.format("##teamcity[publishArtifacts '%s => %s']", tempDiffImage.getAbsolutePath(), Constants.ARTIFACTS_RESULT_PATH + publishToFolder));
                    }
                    
                    log.message(String.format("%s: %s", metric.toUpperCase(), diff.DifferenceAmount));
                    log.message(String.format("##teamcity[buildStatisticValue key='ic_%s_%s' value='%.6f']", artifactName, metric, diff.DifferenceAmount));

                } else {
                    logError(log, problemOnError, String.format("Result for %s: %s", metric.toUpperCase(), diff.StandardOut));
                }

                if(build.getInterruptReason() != null) {
                    return false;
                }
            }

            if("true".equals(params.get(Constants.FEATURE_SETTING_GENERATE_ANIMATED))) {
                String artifactNameAnimated = String.format("%s_animated.webp", FilenameUtils.removeExtension(artifactName));
                File tempAnimatedImage = removeArchive(new File(diffImagesTemp, artifactNameAnimated));

                File[] images = new File[] { referenceImage, newImage };
                String[] annotations = new String[] { String.format("Baseline: #%s", build.getBuildNumber()), String.format("This build: #%s", referenceVersion) };

                if(imageMagickAnimate(diffImagesTemp, log, magick, images, annotations, tempAnimatedImage)) {
                    log.message(String.format("##teamcity[publishArtifacts '%s => %s']", tempAnimatedImage.getAbsolutePath(), Constants.ARTIFACTS_RESULT_PATH + publishToFolder));
                } else {
                    logError(log, problemOnError, String.format("Webp creation failed for %s", tempAnimatedImage.getAbsolutePath()));
                }
            }
        } finally {
            log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
        }

        return true;
    }

    File removeArchive(File inputFile) {

        String input = inputFile.getAbsolutePath();

        //if it's an archive, change the outputs so they go to a folder instead. this is a workaround
        //for repeated publishes to the same output archive recreating that archive every publish
        Pattern archivePtn = Pattern.compile("(!$|![\\\\|/])"); //must end with ! or contain !\ or !/
        Matcher mtch = archivePtn.matcher(input);
        if(mtch.find()) {
            //remove '!'
            input = input.substring(0, mtch.start()) + input.substring(mtch.start() + 1);

            //swap '.' for '_'
            int archiveDot = input.lastIndexOf(".", mtch.start());
            if(archiveDot != -1) {
                input = input.substring(0, archiveDot) + "_" + input.substring(archiveDot + 1);
            }
        }

        return new File(input);
    }

    DiffResult imageMagickDiff(File toolPath, String metric, File referenceImage, File newImage, File createDifferenceImage) {
        
        CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
        cmdLine.addArguments("compare -quality 100 -metric");
        cmdLine.addArgument(metric);
        cmdLine.addArgument(referenceImage.getAbsolutePath());
        cmdLine.addArgument(newImage.getAbsolutePath());
        cmdLine.addArgument(createDifferenceImage.getAbsolutePath());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outStream));
        
        //we don't actually care about the exit code unless it's 2, in which case something has genuinely gone wrong
        executor.setExitValues(new int[] { 0, 1 });

        executor.setWatchdog(new ExecuteWatchdog(60000));
        try {
            executor.execute(cmdLine);
            return new DiffResult(false, outStream.toString());
        } catch (Exception e) {
            return new DiffResult(true, String.format("Exception when comparing:\r\n%s\r\nOutput: %s", e.toString(), outStream));
        }
    }

    boolean imageMagickAnimate(File tempDirectory, BuildProgressLogger log, File toolPath, File[] images, String[] annotations, File createImage) {
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outStream));
        executor.setWatchdog(new ExecuteWatchdog(60000));

        //annotate each image
        for (int i = 0; i < images.length; i++) {

            File tempFile = new File(tempDirectory, "draw.txt");
            Util.writeStrToFile(tempFile, String.format("text 10,10 '%s'", annotations[i]));

            CommandLine cmdLine = new CommandLine(toolPath.getAbsolutePath());
            cmdLine.addArgument("mogrify"); //modify in-place
            cmdLine.addArguments("-quality 100 -background #888f -gravity north -splice 0x43 -gravity northwest -pointsize 20 -draw @" + tempFile.getAbsolutePath());
            cmdLine.addArgument(images[i].getAbsolutePath());

            try {
                executor.execute(cmdLine);
            } catch (Throwable e) {
                log.error(e.toString());
                log.error("Output: " + outStream.toString());
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
        } catch (Throwable e) {
            log.error(e.toString());
            log.error("Output: " + outStream.toString());
            return false;
        }
    }
}