package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.serverSide.ProjectManager;

import novemberdobby.teamcity.imageComp.common.Constants;

public class ImagesTab extends BuildTypeTab {

    String m_resourcePath;
    WebLinks m_links;

    public ImagesTab(WebControllerManager manager, ProjectManager projManager, PluginDescriptor descriptor, WebLinks links) {
        super(Constants.TAB_ID, Constants.TAB_TITLE, manager, projManager, descriptor.getPluginResourcesPath("view_results.jsp"));
        m_resourcePath = descriptor.getPluginResourcesPath();
        m_links = links;
    }

    @Override
    protected void fillModel(Map<String, Object> model, HttpServletRequest request, SBuildType buildType, SUser user) {
        model.put("resources", m_resourcePath);
        model.put("buildTypeIntID", buildType.getBuildTypeId());
        model.put("buildTypeExtID", buildType.getExternalId());
        model.put("projectIntId", buildType.getProjectId());

        //add the address for viewing this tab, it could all be done in JS but ew
        model.put("viewTypeImageCompUrl", String.format("%s&tab=%s", m_links.getConfigurationHomePageUrl(buildType), Constants.TAB_ID));
        
        model.put("viewTypeStatsUrl", String.format("%s&tab=buildTypeStatistics", m_links.getConfigurationHomePageUrl(buildType)));
        model.put("viewProjectStatsUrl", String.format("%s&tab=stats", m_links.getProjectPageUrl(buildType.getProjectExternalId())));
    }

    @Override
    public boolean isAvailable(HttpServletRequest request) {

        SUser user = SessionUser.getUser(request);
        
        //basic check, doesn't mean there'll necessarily be anything to look at
        SBuildType type = getBuildType(request);
        return type != null && user.isPermissionGrantedForProject(type.getProjectId(), Permission.VIEW_PROJECT) && !type.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID).isEmpty();
    }
}