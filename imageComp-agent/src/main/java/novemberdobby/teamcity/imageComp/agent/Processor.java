package novemberdobby.teamcity.imageComp.agent;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
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
                    restrict = String.format(",tag:", params.get(Constants.FEATURE_SETTING_TAG));
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
                    log.message(String.format("Source build is %s", sourceBuildID));

                    File tempDir = build.getBuildTempDirectory();
                    List<String> paths = Arrays.asList(pathsParam.split("[\n\r]"));
                    for (String path : paths) {
                        log.message(String.format("Downloading %s", path));
                    }
                } else {
                    log.error("Skipping downloads due to errors");
                }
            }
        }

        log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
    }
}