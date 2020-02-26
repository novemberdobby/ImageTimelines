package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.serverSide.ProjectManager;

import novemberdobby.teamcity.imageComp.common.Constants;

public class ImagesTab extends BuildTypeTab {

    public ImagesTab(WebControllerManager manager, ProjectManager projManager) {
        super(Constants.TAB_ID, Constants.TAB_TITLE, manager, projManager);
    }

    @Override
    protected void fillModel(Map<String, Object> model, HttpServletRequest request, SBuildType buildType, SUser user) {
        SFinishedBuild build = buildType.getLastChangesFinished();
        if(build == null)
        {
            return;
        }

        BuildArtifacts artifacts = build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT);
        //TODO: display...stuff
    }

    @Override
    public boolean isAvailable(HttpServletRequest request) {

        SUser user = SessionUser.getUser(request);
        
        //basic check, doesn't mean there'll necessarily be anything to look at
        SBuildType type = getBuildType(request);
        return type != null && user.isPermissionGrantedForProject(type.getProjectId(), Permission.VIEW_PROJECT);
    }
}