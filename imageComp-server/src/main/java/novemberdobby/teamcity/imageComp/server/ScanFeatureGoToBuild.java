package novemberdobby.teamcity.imageComp.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import novemberdobby.teamcity.imageComp.common.Constants;

public class ScanFeatureGoToBuild extends BaseController {

    SBuildServer m_server;
    PluginDescriptor m_descriptor;

    public ScanFeatureGoToBuild(SBuildServer server, PluginDescriptor descriptor, WebControllerManager web) {
        m_server = server;
        m_descriptor = descriptor;
        web.registerController(Constants.FEATURE_GO_TO_BUILD_POPUP_URL, this);
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        SUser user = SessionUser.getUser(request);
        Long buildId;
        try {
            buildId = Long.parseLong(request.getParameter("buildId"));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid buildId");
            return null;
        }
        
        SBuild build = m_server.findBuildInstanceById(buildId);
        String bType = build != null ? build.getProjectId() : null;

        if(bType == null || !user.isPermissionGrantedForProject(bType, Permission.VIEW_PROJECT)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        ModelAndView mv = new ModelAndView(m_descriptor.getPluginResourcesPath(Constants.FEATURE_GO_TO_BUILD_POPUP_JSP));
        mv.addObject("build", build);
        mv.addObject("desc", String.format("%s: %s", build.getFullName(), build.getBuildNumber()));
        return mv;
    }
}