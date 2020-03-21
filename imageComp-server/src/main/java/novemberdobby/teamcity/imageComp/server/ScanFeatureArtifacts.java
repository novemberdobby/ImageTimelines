package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import novemberdobby.teamcity.imageComp.common.Constants;

public class ScanFeatureArtifacts extends BaseController {

    SBuildServer m_server;
    PluginDescriptor m_descriptor;

    public ScanFeatureArtifacts(SBuildServer server, PluginDescriptor descriptor, WebControllerManager web) {
        m_server = server;
        m_descriptor = descriptor;
        web.registerController(Constants.FEATURE_ARTIFACTS_POPUP_URL, this);
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        SUser user = SessionUser.getUser(request);
        String bTypeStr = request.getParameter("buildType");
        SBuildType bType = m_server.getProjectManager().findBuildTypeByExternalId(bTypeStr);

        if(bType == null || !user.isPermissionGrantedForProject(bType.getProjectId(), Permission.VIEW_PROJECT)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        ModelAndView mv = new ModelAndView(m_descriptor.getPluginResourcesPath(Constants.FEATURE_ARTIFACTS_POPUP_JSP));
        Map<String, Object> model = mv.getModel();

        SFinishedBuild lastBuild = bType.getLastChangesFinished();
        model.put("lastBuildID", lastBuild == null ? -1 : lastBuild.getBuildId());

        return mv;
    }

}