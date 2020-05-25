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
import novemberdobby.teamcity.imageComp.common.Constants;

public class ImagesTab extends BuildTypeTab {

    String m_resourcePath;

    public ImagesTab(WebControllerManager manager, ProjectManager projManager, PluginDescriptor descriptor) {
        super(Constants.TAB_ID, Constants.TAB_TITLE, manager, projManager, descriptor.getPluginResourcesPath(Constants.MAIN_JSP));
        m_resourcePath = descriptor.getPluginResourcesPath();
    }

    @Override
    protected void fillModel(Map<String, Object> model, HttpServletRequest request, SBuildType buildType, SUser user) {
        StandalonePage.populateModel(model, buildType, m_resourcePath, false);
    }

    @Override
    public boolean isAvailable(HttpServletRequest request) {

        SUser user = SessionUser.getUser(request);
        
        //basic check, doesn't mean there'll necessarily be anything to look at
        SBuildType type = getBuildType(request);
        return type != null && user.isPermissionGrantedForProject(type.getProjectId(), Permission.VIEW_PROJECT) && !type.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID).isEmpty();
    }
}