package novemberdobby.teamcity.imageComp.common;

public class Constants {
    public static final String TAB_ID = "image_comp_tab";
    public static final String TAB_TITLE = "Image comparison";

    public static final String FEATURE_SETTINGS_JSP = "scan_settings.jsp";
    public static final String FEATURE_DISPLAY_NAME = "Image comparison";
    public static final String FEATURE_TYPE_ID = "image_comp_feature";
    public static final String FEATURE_SETTING_ARTIFACTS = "artifact_paths";
    public static final String FEATURE_SETTING_COMPARE_TYPE = "compare_with";
    public static final String FEATURE_SETTING_TAG = "tag_name";
    public static final String FEATURE_SETTING_DIFF_METRIC = "diff_type";
    public static final String FEATURE_SETTING_DIFF_METRIC_DEFAULT = "dssim";
    public static final String FEATURE_SETTING_HIDDEN_REQ_IM = "hidden_req_im";
    public static final String FEATURE_SETTING_GENERATE_GIF = "generate_gif";

    public static final String TOOL_IM_PATH_PARAM = "teamcity.tool.ImageMagick";
    public static final String TOOL_BC_PATH_PARAM = "teamcity.tool.BeyondCompare";
    
    public static final String ARTIFACTS_RESULT_PATH = "image_comparisons";
}