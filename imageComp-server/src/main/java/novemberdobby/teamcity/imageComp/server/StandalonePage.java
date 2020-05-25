package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.BuildType;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import novemberdobby.teamcity.imageComp.common.Constants;

public class StandalonePage extends BaseController {

    SBuildServer m_server;
    PluginDescriptor m_descriptor;

    public StandalonePage(SBuildServer server, WebControllerManager web, PluginDescriptor descriptor) {
        m_server = server;
        m_descriptor = descriptor;
        web.registerController(Constants.STANDALONE_PAGE_URL, this);
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        SUser user = SessionUser.getUser(request);
        ModelAndView mv = new ModelAndView(m_descriptor.getPluginResourcesPath(Constants.MAIN_JSP));
        Map<String, Object> model = mv.getModel();

        String buildTypeStr = request.getParameter("buildType");
        ProjectManager pm = m_server.getProjectManager();
        BuildType buildType = pm.findBuildTypeByExternalId(buildTypeStr);

        if(buildType == null || !user.isPermissionGrantedForProject(buildType.getProjectId(), Permission.VIEW_PROJECT)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
            return null;
        }

        populateModel(model, buildType, m_descriptor.getPluginResourcesPath(), true);
        return mv;
    }
    
    public static void populateModel(Map<String, Object> model, BuildType buildType, String resourcePath, boolean externalPage) {

        model.put("external", externalPage);
        model.put("title", buildType.getFullName());
        model.put("resources", resourcePath);
        model.put("buildTypeIntID", buildType.getBuildTypeId());
        model.put("buildTypeExtID", buildType.getExternalId());
        model.put("projectIntId", buildType.getProjectId());

        RelativeWebLinks links = new RelativeWebLinks();
        model.put("viewTypeStatsUrl", String.format("%s&tab=buildTypeStatistics", links.getConfigurationHomePageUrl(buildType)));
        model.put("viewProjectStatsUrl", String.format("%s&tab=stats", links.getProjectPageUrl(buildType.getProjectExternalId())));
    }
}