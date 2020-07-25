package novemberdobby.teamcity.imageComp.common;

public class Constants {
    public static final String TAB_ID = "image_comp_tab";
    public static final String TAB_TITLE = "Image timelines";

    public static final String MAIN_JSP = "view_results.jsp";

    public static final String FEATURE_SETTINGS_JSP = "scan_settings.jsp";
    public static final String FEATURE_DISPLAY_NAME = "Image timelines";
    public static final String FEATURE_TYPE_ID = "image_comp_feature";
    public static final String FEATURE_SETTING_ARTIFACTS = "artifact_paths";
    public static final String FEATURE_SETTING_COMPARE_TYPE = "compare_with";
    public static final String FEATURE_SETTING_TAG = "tag_name";
    public static final String FEATURE_SETTING_DIFF_METRIC = "diff_type";
    public static final String FEATURE_SETTING_DIFF_METRIC_DEFAULT = "dssim";
    public static final String FEATURE_SETTING_GENERATE_ANIMATED = "generate_animated";
    public static final String FEATURE_SETTING_FAIL_ON_ERROR = "fail_on_error";

    public static final String FEATURE_ARTIFACTS_POPUP_URL = "/imageCompArtifactsPopup.html";
    public static final String FEATURE_ARTIFACTS_POPUP_JSP = "scan_settings_artifacts.jsp";

    public static final String REDIRECT_TAB_ID = "build_to_type_redirect";
	public static final String REDIRECT_TAB_JSP = "redirect_to_type_tab.jsp";
    
    public static final String ARTIFACTS_RESULT_PATH = "image_comparisons";

    public static final String STANDALONE_PAGE_URL = "/image_timelines.html";
    public static final String FEATURE_REFERENCE_BUILD_URL = "/image_timeline_reference.html";
}