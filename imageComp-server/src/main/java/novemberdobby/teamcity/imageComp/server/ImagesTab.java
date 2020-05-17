package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import novemberdobby.teamcity.imageComp.common.Constants;

public class ImagesTab extends BuildTypeTab {

    String m_resourcePath;

    public ImagesTab(WebControllerManager manager, ProjectManager projManager, PluginDescriptor descriptor) {
        super(Constants.TAB_ID, Constants.TAB_TITLE, manager, projManager, descriptor.getPluginResourcesPath("view_results.jsp"));
        m_resourcePath = descriptor.getPluginResourcesPath();
    }

    @Override
    protected void fillModel(Map<String, Object> model, HttpServletRequest request, SBuildType buildType, SUser user) {
        model.put("resources", m_resourcePath);
        model.put("buildTypeIntID", buildType.getBuildTypeId());
        model.put("buildTypeExtID", buildType.getExternalId());
        model.put("projectIntId", buildType.getProjectId());

        RelativeWebLinks links = new RelativeWebLinks();
        
        model.put("viewTypeStatsUrl", String.format("%s&tab=buildTypeStatistics", links.getConfigurationHomePageUrl(buildType)));
        model.put("viewProjectStatsUrl", String.format("%s&tab=stats", links.getProjectPageUrl(buildType.getProjectExternalId())));
    }

    @Override
    public boolean isAvailable(HttpServletRequest request) {

        SUser user = SessionUser.getUser(request);
        
        //basic check, doesn't mean there'll necessarily be anything to look at
        SBuildType type = getBuildType(request);
        return type != null && user.isPermissionGrantedForProject(type.getProjectId(), Permission.VIEW_PROJECT) && !type.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID).isEmpty();
    }
}