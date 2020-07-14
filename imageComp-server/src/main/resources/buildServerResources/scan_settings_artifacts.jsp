<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="paths_list" value="<%=Constants.FEATURE_SETTING_ARTIFACTS%>"/>

<div id="artifactsTreeIC"></div>

<script type="text/javascript">
  <c:choose>
    <c:when test="${lastBuildID == -1}">
        alert("No builds found");
    </c:when>
    <c:otherwise>
      ReactUI.renderConnected('artifactsTreeIC', ReactUI.BuildArtifactsTree, {
      buildId: ${lastBuildID},
      canSelectDirs: false,
      showToggleHidden: false,
      onSelect: function(path) {
        var ctl = $('${paths_list}');
        ctl.value = (ctl.value.trimEnd() + '\n' + path).trim();
      }
      });
    </c:otherwise>
    </c:choose>
</script>