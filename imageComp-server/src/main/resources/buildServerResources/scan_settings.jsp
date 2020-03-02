<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="paths_list" value="<%=Constants.FEATURE_SETTING_ARTIFACTS%>"/>
<c:set var="compare_type" value="<%=Constants.FEATURE_SETTING_COMPARE_TYPE%>"/>
<c:set var="tag_name" value="<%=Constants.FEATURE_SETTING_TAG%>"/>
<c:set var="diff_type" value="<%=Constants.FEATURE_SETTING_DIFF_METRIC%>"/>
<c:set var="diff_type_default" value="<%=Constants.FEATURE_SETTING_DIFF_METRIC_DEFAULT%>"/>
<c:set var="hidden_agent_req_im" value="<%=Constants.FEATURE_SETTING_HIDDEN_REQ_IM%>"/>

<c:set var="hidden_agent_req_im_value" value="<%=Constants.TOOL_IM_PATH_PARAM%>"/>

<jsp:useBean id="buildForm" scope="request" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

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
  <th>Comparison method:</th>
  <td>
    <span class="smallNote">From ImageMagick documentation
      <a href="https://imagemagick.org/script/command-line-options.php#metric" rel="nofollow noreferrer" target="_blank" class="actionIconWrapper"><bs:helpIcon/></a>
    </span>

    <%-- multiselect properties are broken (https://youtrack.jetbrains.com/issue/TW-32265), so use a hidden backing prop --%>
    <div style="display: none">
      <props:textProperty name="${diff_type}" value="${diff_type_default}"/>
    </div>

    <select id="diff_type_multi" multiple="true" onchange="BS.ImageComparison.saveDiffTypes()">
      <option value="ae">AE - absolute error count, number of different pixels (-fuzz affected)</option>
      <option value="dssim">DSSIM - structural dissimilarity index</option>
      <option value="fuzz">FUZZ - mean color distance</option>
      <option value="mae">MAE - mean absolute error (normalized), average channel error distance</option>
      <option value="mepp">MEPP - mean error per pixel (normalized mean error, normalized peak error)</option>
      <option value="mse">MSE - mean error squared, average of the channel error squared</option>
      <option value="ncc">NCC - normalized cross correlation</option>
      <option value="pae">PEA - peak absolute (normalized peak absolute)</option>
      <option value="phash">PHASH - perceptual hash for the sRGB and HCLp colorspaces</option>
      <option value="psnr">PSNR - peak signal to noise ratio</option>
      <option value="rmse">RMSE - root mean squared (normalized root mean squared)</option>
      <option value="ssim">SSIM - structural similarity index</option>
    </select>

    <span class="error" id="error_${diff_type}"></span>
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
    },

    loadDiffTypes: function() {
      var types = new Set("${propertiesBean.properties[diff_type]}".split(','));
      var target = $('diff_type_multi');
      for(var i = 0; i < target.options.length; i++) {
        target.options[i].selected = types.has(target.options[i].value);
      }
      if(types.length > 0) {
        target.selectedOptions[0].scrollIntoView()
      }
    },

    saveDiffTypes: function() {
      var selected = $('diff_type_multi');
      var option = "";
      for(var i = 0; i < selected.selectedOptions.length; i++) {
        option += selected.selectedOptions[i].value + ",";
      }
      $('${diff_type}').value = option;
    }
  };
  
  $('${compare_type}')["onkeyup"] = BS.ImageComparison.onComparisonTypeChange;
  $('${compare_type}')["onclick"] = BS.ImageComparison.onComparisonTypeChange;
  
  BS.ImageComparison.onComparisonTypeChange();
  BS.ImageComparison.loadDiffTypes();
</script>