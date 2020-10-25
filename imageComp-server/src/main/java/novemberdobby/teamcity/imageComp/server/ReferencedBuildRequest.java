package novemberdobby.teamcity.imageComp.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.tags.TagsManager;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import novemberdobby.teamcity.imageComp.common.Constants;

/*handle requests with modes:
  -"timeline": timeline view; query which build was the baseline (artifact specific)
  -"process": agent-side diffing; query which build to use as the baseline
  -"preview": feature settings dialog; query which build satisfies the criteria set in the dialog
*/
public class ReferencedBuildRequest extends BaseController {

    SBuildServer m_server;
    TagsManager m_tagsManager;
    SecurityContext m_secContext;

    public ReferencedBuildRequest(SBuildServer server, TagsManager tags, WebControllerManager web, SecurityContext securityContext) {
        m_server = server;
        m_tagsManager = tags;
        m_secContext = securityContext;
        web.registerController(Constants.FEATURE_REFERENCE_BUILD_URL, this);
    }

    private boolean hasPermission(HttpServletRequest request, SBuildType bType) {

        AuthorityHolder authHolder = m_secContext.getAuthorityHolder();
        if(authHolder != null) {
            return authHolder.isPermissionGrantedForProject(bType.getProjectId(), Permission.VIEW_PROJECT);
        }
        
        return false;
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String mode = request.getParameter("mode");
        if (mode != null) {

            //TODO: tickbox for "only successful builds" (only display on last/tagged types)
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