<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<c:set var="generate_animated" value="<%=Constants.FEATURE_SETTING_GENERATE_ANIMATED%>"/>
<c:set var="fail_on_problem" value="<%=Constants.FEATURE_SETTING_FAIL_ON_ERROR%>"/>
<c:set var="artifact_popup_url" value="<%=Constants.FEATURE_ARTIFACTS_POPUP_URL%>"/>

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
    <c:set var="text">Artifacts to process for each build. Use <strong>archive.zip!/image.jpg</strong> to access archives</c:set>
    <props:multilineProperty name="${paths_list}" rows="5" cols="70" linkTitle="" expanded="true" note="${text}"/>

    <%-- no artifacts in abstract builds --%>
    <c:if test='${!buildForm.isTemplate()}'>
      <div style="padding-top: 0.5em;">
        <forms:button id="btnShowIcArtifactPicker" onclick="BS.ImageComparison.showArtifactsPopup()" className="btn btn_mini">Pick from last build</forms:button>
      </div>
    </c:if>

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

<tr class="advancedSetting">
  <th>Flicker:</th>
  <td>
    <props:checkboxProperty name="${generate_animated}" value="true"/>
    <label for="${generate_animated}">Generate animated image of differences</label>
  </td>
</tr>

<tr class="advancedSetting">
  <th>Fail build on error:</th>
  <td>
    <props:checkboxProperty name="${fail_on_problem}" />
    <label for="${fail_on_problem}">Add a build problem when any comparisons fail</label>
  </td>
</tr>

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
    },

    showArtifactsPopup: function() {
      if(BS.ImageComparison.ArtifactsPopup == undefined) {
        BS.ImageComparison.ArtifactsPopup = new BS.Popup("addIcArtifactsPopup", {
          hideOnMouseOut: false,
          hideOnMouseClickOutside: true,
          shift: {x: 0, y: 20},
          url: base_uri + "${artifact_popup_url}?buildType=${buildForm.externalId}"
        });
      }

      BS.ImageComparison.ArtifactsPopup.showPopupNearElement($('btnShowIcArtifactPicker'));
    }
  };
  
  $('${compare_type}')["onkeyup"] = BS.ImageComparison.onComparisonTypeChange;
  $('${compare_type}')["onclick"] = BS.ImageComparison.onComparisonTypeChange;
  
  BS.ImageComparison.onComparisonTypeChange();
  BS.ImageComparison.loadDiffTypes();
  BS.ImageComparison.saveDiffTypes();
</script>