package novemberdobby.teamcity.imageComp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

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
        sb.append(paths);
        
        return sb.toString();
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(Constants.FEATURE_SETTING_ARTIFACTS, "");
        return result;
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