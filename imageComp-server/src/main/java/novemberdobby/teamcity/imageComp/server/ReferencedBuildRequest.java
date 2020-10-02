package novemberdobby.teamcity.imageComp.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
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
        //TODO there must be a nicer way to do this! put in a feature request
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

            if(request.getMethod().equals("GET")) {
                switch (mode.toLowerCase()) {
                    case "preview":
                    case "process":
                    {
                        SBuildType buildType = getBuildType(request);
                        if(buildType == null || !hasPermission(request, buildType)) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
                            return null;
                        }
                        
                        Build targetBuild = null;
                        String compareType = request.getParameter(Constants.FEATURE_SETTING_COMPARE_TYPE);
                        switch(compareType) {
                            case "tagged":
                                String tagName = request.getParameter(Constants.FEATURE_SETTING_TAG);
                                if(tagName != null && tagName.length() > 0) {
                                    List<Build> taggedList = m_tagsManager.findAll(tagName, buildType);
                                    for (Build tagged : taggedList) {
                                        if(tagged.isFinished() && !tagged.isPersonal() && tagged.getCanceledInfo() == null) {
                                            targetBuild = tagged;
                                            break;
                                        }
                                    }
                                }
                                break;
            
                            case "last":
                                targetBuild = buildType.getLastChangesFinished();
                                break;

                            case "buildId":
                                targetBuild = getBuild(request, Constants.FEATURE_SETTING_BUILD_ID);
                                break;
                        }
                        
                        if(targetBuild != null) {
                            writeBuild(response, targetBuild);
                        } else {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                        }
                        
                        return null;
                    }

                    case "timeline": //find out which build we downloaded artifacts from
                    {
                        SBuild build = getBuild(request);
                        if(build == null) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build ID");
                            return null;
                        }
                        
                        SBuildType buildType = build.getBuildType();
                        if(buildType == null || !hasPermission(request, buildType)) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build type or no permission to view");
                            return null;
                        }
                
                        String artifact = request.getParameter("artifact");
                        
                        for (Entry<Build, List<ArtifactInfo>> sourceBuild : build.getDownloadedArtifacts().getArtifacts().entrySet()) {

                            //for each comparison, a build downloads artifacts from two jobs: itself and the baseline. disregard self!
                            if(sourceBuild.getKey() == build) {
                                continue;
                            }

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
            } else if(request.getMethod().equals("POST")) {
                switch(mode.toLowerCase()) {
                    case "update_baseline":
                    {
                        SBuild targetBuild = getBuild(request);
                        if(targetBuild == null || targetBuild.getBuildType() == null || !hasPermission(request, targetBuild.getBuildType())) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid build or no permission to view");
                            return null;
                        }

                        SBuildType config = targetBuild.getBuildType();

                        //find the first feature which compares the selected artifact
                        String targetArtifact = request.getParameter("artifact");
                        for (SBuildFeatureDescriptor feature : config.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID)) {
                            Map<String, String> params = feature.getParameters();
                            boolean updateThisFeature = false;

                            String artifactsParam = params.get(Constants.FEATURE_SETTING_ARTIFACTS);
                            List<String> artifacts = Arrays.asList(artifactsParam.split("[\n\r]"));
                            for(String artifact : artifacts) {
                                if(targetArtifact.equalsIgnoreCase(artifact)) {
                                    updateThisFeature = true;
                                    break;
                                }
                            }

                            if(updateThisFeature) {
                                response.setContentType("text/plain");
                                PrintWriter writer = response.getWriter();
                                params = new HashMap<String, String>(params); //writeable
                                params.put(Constants.FEATURE_SETTING_BUILD_ID, Long.toString(targetBuild.getBuildId()));
                                String oldCompareType = params.put(Constants.FEATURE_SETTING_COMPARE_TYPE, "buildId");

                                if(params.equals(feature.getParameters())) {
                                    writer.write("Nothing to update.");
                                } else {
                                    config.updateBuildFeature(feature.getId(), Constants.FEATURE_TYPE_ID, params);
                                    config.persist(); //logged as build_type_edit_settings by the correct user
                                    writer.write(String.format("Successfully set baseline image to build ID %s (#%s)", targetBuild.getBuildId(), targetBuild.getBuildNumber()));
                                    if(oldCompareType == null || !oldCompareType.equals("buildId")) {
                                        writer.write("\n(comparisons were not previously using a specific build ID, this also has been changed)");
                                    }
                                }

                                break;
                            }
                        }

                        return null;
                    }
                }
            }
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mode for GET must be 'process', 'preview' or 'timeline'. Mode for POST must be 'update_baseline'.");
        return null;
    }

    void writeBuild(HttpServletResponse response, Build sourceBuild) throws IOException {
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.write(Long.toString(sourceBuild.getBuildId()));
        writer.write(",");
        writer.write(sourceBuild.getBuildNumber());
    }
    
    SBuild getBuild(HttpServletRequest request) {
        return getBuild(request, "buildId");
    }

    SBuild getBuild(HttpServletRequest request, String paramName) {
        Long buildId = -1L;
        try {
            buildId = Long.parseLong(request.getParameter(paramName));
        } catch (NumberFormatException e) { }

        return m_server.findBuildInstanceById(buildId);
    }

    SBuildType getBuildType(HttpServletRequest request) {
        String buildTypeStr = request.getParameter("buildTypeId");
        return m_server.getProjectManager().findBuildTypeByExternalId(buildTypeStr);
    }
}