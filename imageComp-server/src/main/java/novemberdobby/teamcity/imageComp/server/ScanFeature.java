package novemberdobby.teamcity.imageComp.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
        
        sb.append("Generate comparisons to the last finished build");
        if("tagged".equals(params.get(Constants.FEATURE_SETTING_COMPARE_TYPE))) {
            sb.append(String.format(" with tag '%s'", params.get(Constants.FEATURE_SETTING_TAG)));
        }

        sb.append(" for:\r\n");
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
        return Collections.singleton(new Requirement("image_conversion_tool", Constants.TOOL_PATH_PARAM, null, RequirementType.EXISTS)); //TODO: update to IM
    }
    
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return new FeatureValidator();
    }

    static class FeatureValidator implements PropertiesProcessor {
        
        @Override
        public Collection<InvalidProperty> process(Map<String, String> params) {
            
            ArrayList<InvalidProperty> result = new ArrayList<InvalidProperty>();

            //TODO: bad syntax on paths? each should be a single file
            
            //check for duplicates - it could still be broken if they really tried, via parameters with the same values
            String pathSettings = params.get(Constants.FEATURE_SETTING_ARTIFACTS);
            List<String> paths = Arrays.asList(pathSettings.split("[\n\r]"));
            Set<String> pathsSet = new HashSet<String>(paths);
            if(paths.size() != pathsSet.size()) {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, String.format("Paths list cannot contain duplicates, %s found", paths.size() - pathsSet.size())));
            }

            //check tag, note that this doesn't guarantee it'll be valid
            if("tagged".equals(params.get(Constants.FEATURE_SETTING_COMPARE_TYPE))) {
                String tag = params.get(Constants.FEATURE_SETTING_TAG);
                if(tag == null || tag.length() == 0 || tag.contains(" ")) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_TAG, "Invalid tag - must be a valid string with no spaces"));
                }
            }

            return result;
        }
    }
}