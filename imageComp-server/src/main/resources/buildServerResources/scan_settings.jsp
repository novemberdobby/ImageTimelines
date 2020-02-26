<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="paths_list" value="<%=Constants.FEATURE_SETTING_ARTIFACTS%>"/>

<jsp:useBean id="buildForm"  scope="request" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"/>

<tr class="noBorder">
  <th>Images:</th>
  <td>
    <props:multilineProperty name="${paths_list}" rows="5" cols="70" linkTitle="" expanded="true" note="Artifacts to process for each build">
      <jsp:attribute name="afterTextField">
        <bs:agentArtifactsTree fieldId="${paths_list}" buildTypeId="${buildForm.externalId}"/>
      </jsp:attribute>
    </props:multilineProperty>
  </td>
</tr>
