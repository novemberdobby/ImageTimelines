package novemberdobby.teamcity.imageComp.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

import novemberdobby.teamcity.imageComp.common.Constants;
import novemberdobby.teamcity.imageComp.common.Util;

public class ScanFeature extends BuildFeature {

    private String m_jspPath;
    private SBuildServer m_server;
    
    //TODO: option to arbitrarily compare builds, requires comparison on the server (slow, need to restrict)
    //TODO: dedicated project tab that supports many build types too?
    
    public ScanFeature(PluginDescriptor descriptor, SBuildServer server) {
        m_server = server;
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
        
        sb.append("Generate comparisons to the ");
        String compareType = params.get(Constants.FEATURE_SETTING_COMPARE_TYPE);
        if("last".equals(compareType)) {
            sb.append("last finished build");
        } else if("tagged".equals(compareType)) {
            sb.append(String.format("last finished build with tag '%s'", params.get(Constants.FEATURE_SETTING_TAG)));
        } else if("buildId".equals(compareType)) {
            sb.append(String.format("build with ID %s", params.get(Constants.FEATURE_SETTING_BUILD_ID)));
        }
        
        sb.append(" for:\r\n");
        if(paths == null) {
            sb.append("<none>");
        } else {
            sb.append(paths);
        }
        
        sb.append("\r\n\r\n");
        sb.append(String.format("Compare using methods: %s", String.join(", ", Util.getCompareMetrics(params)).toUpperCase()));

        if("true".equals(params.get(Constants.FEATURE_SETTING_FAIL_ON_ERROR))) {
            sb.append("\r\n\r\n");
            sb.append("Fail the build on any comparison error");
        }
        
        return sb.toString();
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(Constants.FEATURE_SETTING_ARTIFACTS, "");
        result.put(Constants.FEATURE_SETTING_DIFF_METRIC, Constants.FEATURE_SETTING_DIFF_METRIC_DEFAULT);
        result.put(Constants.FEATURE_SETTING_GENERATE_ANIMATED, "true");
        result.put(Constants.FEATURE_SETTING_FAIL_ON_ERROR, "false");
        return result;
    }
    
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return new FeatureValidator(m_server);
    }

    static class FeatureValidator implements PropertiesProcessor {
        
        SBuildServer m_server;

        public FeatureValidator(SBuildServer server) {
            m_server = server;
        }

        @Override
        public Collection<InvalidProperty> process(Map<String, String> params) {
            
            ArrayList<InvalidProperty> result = new ArrayList<InvalidProperty>();
            
            //check for duplicates - it could still be broken if they really tried, via parameters with the same values
            String pathSettings = params.get(Constants.FEATURE_SETTING_ARTIFACTS);
            if(pathSettings != null) {
                List<String> paths = Arrays.asList(pathSettings.split("[\n\r]"));
                Set<String> pathsSet = new HashSet<String>(paths);
                if(paths.size() != pathsSet.size()) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, String.format("Paths list cannot contain duplicates, %s found", paths.size() - pathsSet.size())));
                }

                Pattern checkArchives = Pattern.compile("![^/]");
                for (String path : paths) {
                    Matcher mtch = checkArchives.matcher(path);
                    if(mtch.find()) {
                        result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, String.format("Paths inside archives must contain '!/', see note (%s)", path)));
                    }
                }
            } else {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_ARTIFACTS, "Paths list cannot be empty"));
            }

            String compareType = params.get(Constants.FEATURE_SETTING_COMPARE_TYPE);
            if("last".equals(compareType)) {

            }
            else if("tagged".equals(compareType)) { //check tag, note that this doesn't guarantee it'll be valid
                String tag = params.get(Constants.FEATURE_SETTING_TAG);
                if(tag == null || tag.length() == 0 || tag.contains(" ")) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_TAG, "Invalid tag - must be a valid string with no spaces"));
                }
            }
            else if("buildId".equals(compareType)) { //check baseline build currently exists
                String buildIdStr = params.get(Constants.FEATURE_SETTING_BUILD_ID);
                Long buildId = -1L;
                try {
                    buildId = Long.parseLong(buildIdStr);
                } catch (NumberFormatException e) { }

                if(m_server.findBuildInstanceById(buildId) == null) {
                    result.add(new InvalidProperty(Constants.FEATURE_SETTING_BUILD_ID, String.format("Unknown baseline build '%s'", buildIdStr)));
                }
            }
            else {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_TAG, String.format("Unknown comparison type '%s'", compareType)));
            }

            String diffMetric = params.get(Constants.FEATURE_SETTING_DIFF_METRIC);
            if(diffMetric == null || diffMetric.equals("")) {
                result.add(new InvalidProperty(Constants.FEATURE_SETTING_DIFF_METRIC, "One or more diff metrics must be selected"));
            }

            return result;
        }
    }
}