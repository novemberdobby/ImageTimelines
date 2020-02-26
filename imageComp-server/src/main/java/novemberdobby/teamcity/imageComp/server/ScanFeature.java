package novemberdobby.teamcity.imageComp.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

import novemberdobby.teamcity.imageComp.common.Constants;

public class ScanFeature extends BuildFeature {

    private String m_jspPath;
    
    public ScanFeature(PluginDescriptor descriptor) {
        m_jspPath = descriptor.getPluginResourcesPath(Constants.FEATURE_SETTINGS_JSP);
    }
    
    @Override
    public String getDisplayName() {
        return Constants.FEATURE_DISPLAY_NAME;
    }

    @Override
    public String getEditParametersUrl() {
        return m_jspPath;
    }

    @Override
    public String getType() {
        return Constants.FEATURE_TYPE_ID;
    }
    
    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return true; //they may want to disable processing on different artifacts separately
    }
    
    @Override
    public String describeParameters(Map<java.lang.String, java.lang.String> params) {
        
        StringBuilder sb = new StringBuilder();
        String paths = params.get(Constants.FEATURE_SETTING_ARTIFACTS);
        
        sb.append("Generate comparisons for:\r\n");
        if(paths == null) {
            sb.append("<none>");
        } else {
            sb.append(paths);
        }
        
        return sb.toString();
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(Constants.FEATURE_SETTING_ARTIFACTS, "");
        return result;
    }

    @Override
    public Collection<Requirement> getRequirements(Map<String, String> params) {
        return Collections.singleton(new Requirement("image_conversion_tool", "teamcity.tool.maven", null, RequirementType.EXISTS)); //TODO: update to IM
    }
    
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return new FeatureValidator();
    }

    static class FeatureValidator implements PropertiesProcessor {
        
        @Override
        public Collection<InvalidProperty> process(Map<String, String> input) {
            
            ArrayList<InvalidProperty> result = new ArrayList<InvalidProperty>();

            //TODO: what could make them invalid? bad syntax on paths?

            return result;
        }
    }
}