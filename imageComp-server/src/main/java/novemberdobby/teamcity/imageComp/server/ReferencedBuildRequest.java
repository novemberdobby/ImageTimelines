package novemberdobby.teamcity.imageComp.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import jetbrains.buildServer.tags.TagsManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import novemberdobby.teamcity.imageComp.common.Constants;

/*handle requests with modes:
  -"timeline": timeline view; query which build was the baseline (artifact specific)
  -"process": agent-side diffing; query which build to use as the baseline
  -"preview": feature settings dialog; query which build satisfies the criteria set in the dialog
*/
public class ReferencedBuildRequest extends BaseController {

    SBuildServer m_server;
    TagsManager m_tagsManager;

    public ReferencedBuildRequest(SBuildServer server, TagsManager tags, WebControllerManager web) {
        m_server = server;
        m_tagsManager = tags;
        web.registerController(Constants.FEATURE_REFERENCE_BUILD_URL, this);
    }

    private boolean hasPermission(HttpServletRequest request, SBuildType bType) {
        SUser user = SessionUser.getUser(request);

        //real user? check normally
        if(user != null) {
            return user.isPermissionGrantedForProject(bType.getProjectId(), Permission.VIEW_PROJECT);
        }

        //if user is null but we're still handling the request, they must have authenticated correctly.
        //one case where that's possible is a transient access user valid while the build is running,
        //so check if the username matches a build of the build type we're dealing with.
        //TODO there must be a nicer way to do this!
        String auth = request.getHeader("authorization");
        if(auth != null && auth.startsWith("Basic ")) {
            auth = auth.substring("Basic ".length());
            try {
                auth = new String(Base64.getDecoder().decode(auth));
            } catch (IllegalArgumentException e) {
                return false;
            }

            Pattern authPtn = Pattern.compile("TeamCityBuildId=(?<id>\\d+):\\w{32}");
            Matcher mtch = authPtn.matcher(auth);
            if(mtch.matches()) {
                Long buildId = Long.parseLong(mtch.group("id"));
                SBuild build = m_server.findBuildInstanceById(buildId);
                return build != null && build.getBuildTypeId().equals(bType.getBuildTypeId());
            }
        }

        return false;
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {

        //TODO: are builds that get used for artifacts preserved past cleanup?
        String mode = request.getParameter("mode");
        if (mode != null) {

            switch (mode.toLowerCase()) {
                case "preview":
                case "process":
                    String buildTypeStr = request.getParameter("buildTypeId");
                    SBuildType buildType = m_server.getProjectManager().findBuildTypeByExternalId(buildTypeStr);

                    if(buildType == null || !hasPermission(request, buildType)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
                        return null;
                    }
                    
                    Build targetBuild = null;
                    String compareType = request.getParameter(Constants.FEATURE_SETTING_COMPARE_TYPE);
                    switch(compareType) {
                        case "tagged":
                            List<Build> taggedList = m_tagsManager.findAll(request.getParameter("tag"), buildType);
                            for (Build tagged : taggedList) {
                                if(tagged.isFinished() && !tagged.isPersonal() && tagged.getCanceledInfo() == null) {
                                    targetBuild = tagged;
                                    break;
                                }
                            }
                            break;
        
                        case "last":
                            targetBuild = buildType.getLastChangesFinished();
                            break;
                    }
                    
                    if(targetBuild != null) {
                        writeBuild(response, targetBuild);
                        return null;
                    }
                    
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                    return null;

                case "timeline": //find out which build we downloaded artifacts from
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
                    if(type == null || !hasPermission(request, type)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
                        return null;
                    }
            
                    String artifact = request.getParameter("artifact");
                    
                    for (Entry<Build, List<ArtifactInfo>> sourceBuild : build.getDownloadedArtifacts().getArtifacts().entrySet()) {
                        for (ArtifactInfo art : sourceBuild.getValue()) {
                            if(artifact.equals(art.getArtifactPath())) {
                                writeBuild(response, sourceBuild.getKey());
                                return null;
                            }
                        }
                    }
                    
                    //404 for artifact not found
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Couldn't find source build for artifact");
                    return null;
            }
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mode must be 'process', 'preview' or 'timeline'");
        return null;
    }

    void writeBuild(HttpServletResponse response, Build sourceBuild) throws IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.write(Long.toString(sourceBuild.getBuildId()));
        writer.write(",");
        writer.write(sourceBuild.getBuildNumber());
    }
}