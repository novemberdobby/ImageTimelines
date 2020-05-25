package novemberdobby.teamcity.imageComp.server;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ArtifactInfo;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import novemberdobby.teamcity.imageComp.common.Constants;

//handle requests for "where did downloaded artifact X come from", which is stored in the DB (dbo.downloaded_artifacts)
//this may break if people add artifact dependencies manually that have overlap with what
//the agent-side feature downloads, but why would you do that
public class DownloadedArtifactsRequest extends BaseController {

    SBuildServer m_server;

    public DownloadedArtifactsRequest(SBuildServer server, WebControllerManager web) {
        m_server = server;
        web.registerController(Constants.ARTIFACT_LOOKUP_URL, this);
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        SUser user = SessionUser.getUser(request);

        Long buildId = -1L;
        try {
            buildId = Long.parseLong(request.getParameter("buildId"));
        } catch (Exception e) { }

        SBuild build = m_server.findBuildInstanceById(buildId);
        if(build == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build ID");
            return null;
        }

        SBuildType type = build.getBuildType();
        if(type == null || !user.isPermissionGrantedForProject(type.getProjectId(), Permission.VIEW_PROJECT)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
            return null;
        }

        String artifact = request.getParameter("artifact");
        
        for (Entry<Build, List<ArtifactInfo>> sourceBuild : build.getDownloadedArtifacts().getArtifacts().entrySet()) {
            for (ArtifactInfo art : sourceBuild.getValue()) {
                if(artifact.equals(art.getArtifactPath())) {
                    PrintWriter writer = response.getWriter();
                    writer.write(Long.toString(sourceBuild.getKey().getBuildId()));
                    writer.write(",");
                    writer.write(sourceBuild.getKey().getBuildNumber());
                    return null;
                }
            }
        }
        
        //404 for artifact not found
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Couldn't find source build for artifact");
        return null;
    }
}