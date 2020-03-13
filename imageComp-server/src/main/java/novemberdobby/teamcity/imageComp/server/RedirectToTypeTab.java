package novemberdobby.teamcity.imageComp.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.web.openapi.BuildTab;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import novemberdobby.teamcity.imageComp.common.Constants;

//build instance tab which redirects to build type's tab
public class RedirectToTypeTab extends BuildTab {

    SBuildServer m_server;
    WebLinks m_links;

    public RedirectToTypeTab(WebControllerManager manager, BuildsManager buildManager, PluginDescriptor descriptor, SBuildServer server, WebLinks links) {
        super(Constants.REDIRECT_TAB_ID, Constants.TAB_TITLE, manager, buildManager, descriptor.getPluginResourcesPath(Constants.REDIRECT_TAB_JSP));
        m_server = server;
        m_links = links;
    }

    @Override
    protected void fillModel(Map<String, Object> model, SBuild build) {
        String url = String.format("%s&tab=%s", m_links.getConfigurationHomePageUrl(build.getBuildType()), Constants.TAB_ID);
        model.put("redirect", url);
    }
    
    @Override
    public boolean isAvailable(@NotNull final HttpServletRequest request) {
        String typeId = request.getParameter("buildTypeId");
        SBuildType type = m_server.getProjectManager().findBuildTypeByExternalId(typeId);
        return type != null && !type.getBuildFeaturesOfType(Constants.FEATURE_TYPE_ID).isEmpty();
    }
}