package novemberdobby.teamcity.imageComp.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.EventDispatcher;

import novemberdobby.teamcity.imageComp.common.Constants;

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

            if(pathsParam != null) {
                List<String> paths = Arrays.asList(pathsParam.split("[\n\r]"));
                for (String path : paths) {
                    log.message(path);
                }
            }
        }

        log.activityFinished(blockMsg, "CUSTOM_IMAGE_COMP");
    }
}