<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="paths_list" value="<%=Constants.FEATURE_SETTING_ARTIFACTS%>"/>
<c:set var="compare_type" value="<%=Constants.FEATURE_SETTING_COMPARE_TYPE%>"/>
<c:set var="tag_name" value="<%=Constants.FEATURE_SETTING_TAG%>"/>
<c:set var="hidden_agent_req_im" value="<%=Constants.FEATURE_SETTING_HIDDEN_REQ_IM%>"/>

<c:set var="hidden_agent_req_im_value" value="<%=Constants.TOOL_IM_PATH_PARAM%>"/>

<jsp:useBean id="buildForm"  scope="request" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"/>

<tr class="noBorder">
  <th>Compare against:</th>
  <td>
    <props:selectProperty name="${compare_type}" onchange="BS.ImageComparison.onComparisonTypeChange()">
      <props:option value="last">Last build</props:option>
      <props:option value="tagged">Last build with tag</props:option>
    </props:selectProperty>
  </td>
</tr>

<tr class="noBorder" id="imagecomp.type.custom.tag" style="display: none">
  <th>Tag:</th>
  <td>
    <props:textProperty name="${tag_name}" className="disableBuildTypeParams"/>
    <span class="error" id="error_${tag_name}"></span>
  </td>
</tr>

<tr class="noBorder">
  <th>Images:</th>
  <td>
    <props:multilineProperty name="${paths_list}" rows="5" cols="70" linkTitle="" expanded="true" note="Artifacts to process for each build">
      <%--TODO: list artifacts from last build or something - won't work for files inside zips tho--%>
      <%--<jsp:attribute name="afterTextField">
        <bs:agentArtifactsTree fieldId="${paths_list}" buildTypeId="${buildForm.externalId}"/>
      </jsp:attribute>--%>
    </props:multilineProperty>
    <span class="error" id="error_${paths_list}"></span>
  </td>
</tr>

<%--
  add a hidden property to reference a required tool. build feature requirements aren't enough to make an agent download the tool.
  placed in a div to hide the params dropdown button
  TODO: remove once https://youtrack.jetbrains.com/issue/TW-64761 is fixed
--%>
<div style="display: none">
  <props:textProperty name="${hidden_agent_req_im}" value="%${hidden_agent_req_im_value}%"/>
</div>

<script type="text/javascript">

  BS.ImageComparison = {
    
    onComparisonTypeChange: function() {
      var typeElem = $('${compare_type}');
      var typeValue = typeElem.options[typeElem.selectedIndex].value;
      
      if(typeValue == "tagged")
      {
        BS.Util.show('imagecomp.type.custom.tag');
      }
      else
      {
        BS.Util.hide('imagecomp.type.custom.tag');
      }
      
      //in case the value changes - re-set every time so worst case people only have to re-confirm their feature settings
      $('${hidden_agent_req_im}').value = "%${hidden_agent_req_im_value}%";
      BS.MultilineProperties.updateVisible();
    }
  };
  
  $('${compare_type}')["onkeyup"] = BS.ImageComparison.onComparisonTypeChange;
  $('${compare_type}')["onclick"] = BS.ImageComparison.onComparisonTypeChange;
  
  BS.ImageComparison.onComparisonTypeChange();
</script>