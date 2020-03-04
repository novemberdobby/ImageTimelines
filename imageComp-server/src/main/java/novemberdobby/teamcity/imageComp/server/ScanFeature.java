package novemberdobby.teamcity.imageComp.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import novemberdobby.teamcity.imageComp.common.Util;

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
        
        sb.append("\r\n\r\n");
        sb.append(String.format("Compare using methods: %s", String.join(", ", Util.getCompareMetrics(params))));
        
        return sb.toString();
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(Constants.FEATURE_SETTING_ARTIFACTS, "");
        result.put(Constants.FEATURE_SETTING_DIFF_METRIC, Constants.FEATURE_SETTING_DIFF_METRIC_DEFAULT);
        return result;
    }

    @Override
    public Collection<Requirement> getRequirements(Map<String, String> params) {
        //TODO: instructions for IM tool install in readme https://imagemagick.org/download/binaries/ImageMagick-7.0.9-27-portable-Q16-x64.zip
        //TODO: instructions for BC tool install in readme
        //always require ImageMagick, but decide whether or not to use Beyond Compare at runtime (if it's installed)
        List<Requirement> reqs = new ArrayList<>();
        reqs.add(new Requirement("image_conversion_tool", Constants.TOOL_IM_PATH_PARAM, null, RequirementType.EXISTS));

        //cheeky hack until non-windows agents are supported
        reqs.add(new Requirement("image_conversion_tool_platform", "env.ProgramFiles", null, RequirementType.EXISTS));
        return reqs;
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
            if(pathSettings != null) {
                List<String> paths = Arrays.asList(pathSettings.split("[\n\r]"));
                Set<String> pathsSet = new HashSet<String>(paths);
                if(paths.size() != pathsSet.size()) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, String.format("Paths list cannot contain duplicates, %s found", paths.size() - pathsSet.size())));
                }
            } else {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, "Paths list cannot be empty"));
            }

            //check tag, note that this doesn't guarantee it'll be valid
            if("tagged".equals(params.get(Constants.FEATURE_SETTING_COMPARE_TYPE))) {
                String tag = params.get(Constants.FEATURE_SETTING_TAG);
                if(tag == null || tag.length() == 0 || tag.contains(" ")) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_TAG, "Invalid tag - must be a valid string with no spaces"));
                }
            }

            String diffMetric = params.get(Constants.FEATURE_SETTING_DIFF_METRIC);
            if(diffMetric == null || diffMetric == "") {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_DIFF_METRIC, "One or more diff metrics must be selected"));
            }

            return result;
        }
    }
}