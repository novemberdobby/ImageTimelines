package novemberdobby.teamcity.imageComp;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.serverSide.ProjectManager;

public class ImagesTab extends BuildTypeTab {

    public ImagesTab(WebControllerManager manager, ProjectManager projManager) {
        super(Constants.TAB_ID, Constants.TAB_TITLE, manager, projManager);
    }

    @Override
    protected void fillModel(Map<String, Object> arg0, HttpServletRequest arg1, SBuildType arg2, SUser arg3) {

    }
}