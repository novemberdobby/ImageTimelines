<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="artifact_lookup_url" value="<%=Constants.ARTIFACT_LOOKUP_URL%>"/>
<c:set var="artifact_results_path" value="<%=Constants.ARTIFACTS_RESULT_PATH%>"/>

<script src="${resources}js/Chart.min.2_9_3.js"></script>
<script src="${resources}js/moment.min.2_24_0.js"></script>
<script src="${resources}js/imgslider.min.js"></script>
<link rel="stylesheet" type="text/css" href="${resources}css/imgslider.min.css">

<style type="text/css">
.icOption {
  padding: 0.25em;
  width: min-content;
  margin: 0.25em; 
}
.icLabel {
  overflow: hidden;
  padding: 4px;
  font-weight: bold;
  border-bottom: 2px solid black;
}
.icImage {
  width: 100%;
  display: block;
}
</style>

<div id="statistics_empty" style="display: none;">
  No statistics available.
</div>

<forms:saving id="getImgDataProgress"/>
<div id="img_comp_options" style="display: none; border: 1px solid #868686; border-style: double; margin-bottom: 1em; margin-right: 0.5em; background: #e4e4e4;">
  <div style="padding: 0.25em; width: min-content; margin: 0.25em;">
    Builds
    <forms:saving id="getImgDataProgressBuilds" style="float: right;"/>
    <br>
    <select id="img_comp_opt_count" onchange="BS.ImageCompResults.getData(); BS.ImageCompResults.updateUrl()">
      <option value="100" selected="true">100</option>
      <option value="200">200</option>
      <option value="500">500</option>
      <option value="1000">1000</option>
    </select>
  </div>

  <div class="icOption">
    Artifact
    <br>
    <select id="img_comp_opt_artifact" onchange="BS.ImageCompResults.updateArtifact(); BS.ImageCompResults.updateUrl()"></select>
  </div>

  <div class="icOption">
    Statistic
    <br>
    <select id="img_comp_opt_metric" onchange="BS.ImageCompResults.updateGraph(); BS.ImageCompResults.updateUrl()">
      <option value="-">-</option>
    </select>
  </div>
  
  <div class="icOption">
    View mode
    <br>
    <select id="img_comp_opt_view_mode" onchange="BS.ImageCompResults.updateView(); BS.ImageCompResults.updateUrl()">
      <option value="sxs">Side by side</option>
      <option value="slider">Diff slider</option>
      <option value="diff">Diff image</option>
      <option value="anim">Animated diff</option>
    </select>
  </div>
  
  <div style="margin-left: auto; padding: 1em;">
    <bs:actionsPopup controlId="icActions" popup_options="className: 'quickLinksMenuPopup'">
      <jsp:attribute name="content">
        <div>
          <ul class="menuList">
            <l:li>
              <a href="#" onclick="BS.ImageCompResults.createStatsGraph(false)">Create graph (on build type)</a>
            </l:li>
            <l:li>
              <a href="#" onclick="BS.ImageCompResults.createStatsGraph(true)">Create graph (on parent project)</a>
            </l:li>
          </ul>
        </div>
      </jsp:attribute>
      <jsp:body>Actions</jsp:body>
    </bs:actionsPopup>
    <forms:saving id="createGraphProgress"/>
  </div>
</div>

<div id="statistics_container" style="display: none;">

  <%-- old image on left, new image on right --%>
  <div id="statistics_images_sxs" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="width: 50%; border-right: 1px solid black;">
      <div class="icLabel" style="text-align: left;">
        <a id="img_comp_left_label_sxs" target="_blank"></a>
      </div>
      <a>
        <img class="icImage" id="img_comp_left_sxs">
      </a>
    </div>
    <div style="width: 50%; border-left: 1px solid black;">
      <div class="icLabel" style="text-align: right;">
        <a id="img_comp_right_label_sxs" target="_blank"></a>
      </div>
      <a>
        <img class="icImage" id="img_comp_right_sxs">
      </a>
    </div>
  </div>

  <%-- one large image with slider (starts with old image on left half, new on right half) --%>
  <div id="statistics_images_slider" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="display: flex; border-bottom: 2px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold;">
        <a id="img_comp_left_label_slider" target="_blank"></a>
      </div>
      <div style="overflow: hidden; padding: 4px; font-weight: bold; margin-left: auto;">
        <a id="img_comp_right_label_slider" target="_blank"></a>
      </div>
    </div>

    <div class="slider">
      <div class="slider responsive">
        <div class="left image">
          <img id="img_comp_left_slider" style="display:block;"/>
        </div>
        <div class="right image">
          <img id="img_comp_right_slider" style="display:block;"/>
        </div>
      </div>
    </div>
  </div>
  
  <%-- old image on left, pre-generated difference image in middle, new image on right --%>
  <div id="statistics_images_diff" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="width: 33.33%; border-right: 1px solid black;">
      <div class="icLabel" style="text-align: left;">
        <a id="img_comp_left_label_diff" target="_blank"></a>
      </div>
      <a>
        <img class="icImage" id="img_comp_left_diff">
      </a>
    </div>

    <div style="width: 33.33%; border-right: 1px solid black;">
      <div class="icLabel" style="text-align: center;">
        <a id="img_comp_difference_image" target="_blank"></a>
      </div>
      <a>
        <img class="icImage" id="img_comp_difference">
      <a>
    </div>

    <div style="width: 33.33%; border-left: 1px solid black;">
      <div class="icLabel" style="text-align: right;">
        <a id="img_comp_right_label_diff" target="_blank"></a>
      </div>
      <a>
        <img class="icImage" id="img_comp_right_diff">
      </a>
    </div>
  </div>
  
  <%-- pre-generated animated image of differences --%>
  <div id="statistics_images_anim" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="width: 100%; border-right: 1px solid black;">
      <a>
        <img class="icImage" id="img_comp_anim_diff">
      </a>
    </div>
  </div>

  <div id="img_comp_hint">
    Click a bar on the graph below to display comparison.
  </div>

  <canvas id="stats_chart" height="70em"></canvas>
  <div style="padding-top: 0.5em;">Note: graph extents vary. <span style="color:#ff0000"><strong>Red</strong></span> bars show the highest values in the <strong>currently</strong> visible set.</div>
</div>

<script type="text/javascript">

  BS.ImageCompResults = {

    Artifacts: {},
    CurrentChartData: [],
    SelectedIndex: -1,

    getData: function() {
      if($('img_comp_options').style.display == "none") {
        BS.Util.show('getImgDataProgress');
      } else {
        BS.Util.show('getImgDataProgressBuilds');
      }

      $j.getJSON(base_uri + '/app/rest/builds?locator=buildType(internalId:${buildTypeIntID}),count:' + $('img_comp_opt_count').value + '&fields=build(startDate,id,number,status,buildType(id,name,projectName),statistics(property(name,value)))',
        function(data) {
            BS.ImageCompResults.parseData(data);
        }
      )
    },

    parseData: function(buildData) {
      Artifacts = {};
      for (var i = buildData.build.length - 1; i >= 0; i--) {
        const build = buildData.build[i];
        build.statistics.property.forEach(p => {
          //only get image comp stats
          var match = p.name.match("ic_(.+)_([\\w]+)"); //ic_<artifactname>_<metricname>
          if(match != undefined) {
            var name = match[1];
            if(Artifacts[name] == undefined) {
                Artifacts[name] = {};
            }

            var stat = match[2];
            if(Artifacts[name][stat] == undefined) {
              Artifacts[name][stat] = [];
            }

            Artifacts[name][stat].push({
              number: build.number,
              value: p.value,
              date: new moment(build.startDate),
              id: build.id
            });
          }
        });
      }

      BS.Util.hide('getImgDataProgress');
      BS.Util.hide('getImgDataProgressBuilds');

      if(Object.keys(Artifacts).length == 0) {
        BS.Util.show('statistics_empty');
      } else {
        //fill artifact dropdown
        var ddArtifacts = $('img_comp_opt_artifact');
        var oldArtifact = ddArtifacts.value;
        if(BS.ImageCompResults.SetArtifact != undefined) {
          oldArtifact = BS.ImageCompResults.SetArtifact;
          BS.ImageCompResults.SetArtifact = undefined;
        }

        ddArtifacts.innerHTML = "";
        for(var art in Artifacts) {
          ddArtifacts.options.add(new Option(art, art))
        }
        var newArtifact = ddArtifacts.value;
        ddArtifacts.value = oldArtifact;
        if(ddArtifacts.value == "") {
          ddArtifacts.value = newArtifact;
        }

        BS.Util.show('statistics_container');
        $('img_comp_options').style.display = "flex";
        BS.ImageCompResults.updateArtifact();
      }
    },

    updateArtifact: function() {
      //fill stats dropdown based on selected artifact
      var ddArtifacts = $('img_comp_opt_artifact');
      var ddMetrics = $('img_comp_opt_metric');
      var oldMetric = ddMetrics.value;
      if(BS.ImageCompResults.SetMetric != undefined) {
        oldMetric = BS.ImageCompResults.SetMetric;
        BS.ImageCompResults.SetMetric = undefined;
      }

      ddMetrics.innerHTML = "";

      const targetArtifact = Artifacts[ddArtifacts.value];
      if(targetArtifact != undefined) {
        for(var stat in targetArtifact) {
          ddMetrics.options.add(new Option(stat, stat))
        }
      }
      
      var newMetric = ddMetrics.value;
      ddMetrics.value = oldMetric;
      if(ddMetrics.value == "") {
        ddMetrics.value = newMetric;
      }

      //show initial graph
      BS.ImageCompResults.updateGraph();
    },

    updateGraph: function() {
      $j('.statistics_images').css("display", "none");
      BS.Util.show('img_comp_hint');
      var targetArtifact = $('img_comp_opt_artifact').value;
      var targetMetric = $('img_comp_opt_metric').value;
      
      //TODO: support showing multiple sets e.g. psnr + dssim etc. will need to reintroduce category spacing & make colours show metric
      //set up chart
      var context = document.getElementById('stats_chart').getContext('2d');
      if(BS.ImageCompResults.Chart == undefined) {
        BS.ImageCompResults.Chart = new Chart(context, {
          type: 'bar',
          options: {
            title: { display: true },
            legend: { display: false },
            hover: { animationDuration: 0 },
            animation: { duration: 0 },
            scales: {
              yAxes: [{
                  ticks: { beginAtZero: true }
              }],
              xAxes: [
                {
                  gridLines: { display: false },
                  ticks: {
                    callback: function(value, index, values) {
                      return index % 5 == 0 ? value : '';
                    }
                  }
                }
              ]
            },
            tooltips: {
              displayColors: false,
              callbacks: {
                label: function(tooltipItem, data) {
                  //TODO show metric in multimetric mode, data.datasets[tooltipItem.datasetIndex].label
                  return ["Started " + BS.ImageCompResults.CurrentChartData[tooltipItem.index].date.format("llll")];
                }
              }
            },
            onClick: function(event, items) {
              if(items.length == 1) {
                BS.ImageCompResults.SelectedIndex = items[0]._index;
                BS.ImageCompResults.updateView();
              }
            }
          }
        });
      }

      BS.ImageCompResults.Chart.data.labels.clear();
      BS.ImageCompResults.Chart.data.datasets.clear();

      //collect data
      const target = Artifacts[targetArtifact][targetMetric];
      BS.ImageCompResults.CurrentChartData = target;
      const values = target.map(d => d.value);
      var targetMin = 0; //TODO: is this OK to assume for all metrics?
      var targetMax = values.reduce((a, b) => Math.max(a, b));

      var lerp = function(a, b, c) { return a + (b - a) * c; }
      var invLerp = function(a, b, c) {
        if(b - a == 0) {
          return 0;
        } else {
          return (c - a) / (b - a);
        }
      }
      var colourLerp = function(a, b, c) { return "rgba(" + lerp(a[0], b[0], c) + "," + lerp(a[1], b[1], c) + "," + lerp(a[2], b[2], c) + "," + lerp(a[3], b[3], c) + ")" }

      BS.ImageCompResults.Chart.data.labels = target.map(d => d.number);
      BS.ImageCompResults.Chart.data.datasets = [{
          label: targetMetric,
          data: values,
          backgroundColor: context => {

            //highlight if currently selected
            if(BS.ImageCompResults.SelectedIndex == context.dataIndex) {
              return "rgba(128, 128, 128, 255)";
            }

            var value = context.dataset.data[context.dataIndex];
            var normalised = invLerp(targetMin, targetMax, value);
            if(normalised < 0.5) {
              return colourLerp([0,255,0,255], [255,128,0,255], normalised * 2); //green-orange
            } else {
              return colourLerp([255,128,0,255], [255,0,0,255], (normalised - 0.5) * 2); //orange-red
            }
          },
          hoverBackgroundColor: "rgba(128, 128, 128, 255)",
          categoryPercentage: 1,
          barPercentage: 1.01, //overlap a little so there's no gap. looks a bit silly when there are <10 data points but it's not terrible
          minBarLength: 5,
      }];
      
      BS.ImageCompResults.Chart.options.title.text = "Showing " + values.length + " values";
      BS.ImageCompResults.Chart.update();

      if(values.length > 0) {
        BS.ImageCompResults.SelectedIndex = values.length - 1;
        BS.ImageCompResults.updateView();
      } else {
        BS.ImageCompResults.SelectedIndex = -1;
      }
    },
    
    updateView: function() {
      if(BS.ImageCompResults.SelectedIndex == -1 || BS.ImageCompResults.CurrentChartData == undefined) {
        return;
      }

      BS.ImageCompResults.Chart.update();
      
      var thisBuild = BS.ImageCompResults.CurrentChartData[BS.ImageCompResults.SelectedIndex];
      var artifact = $('img_comp_opt_artifact').value;
      
      //TODO: support dedicated page for more real estate
      //TODO: test with various sized images & mismatched
      //get the build to compare against
      BS.ajaxRequest(window['base_uri'] + '${artifact_lookup_url}', {
        method: "GET",
        parameters: {
          'buildId': thisBuild.id,
          'artifact': artifact,
        },
        onComplete: function(transport) {
          if(transport.status == 200)
          {
            //fill everything out
            var comma = transport.responseText.indexOf(',');
            var baselineId = transport.responseText.substring(0, comma);
            var baselineNumber = transport.responseText.substring(comma + 1);

            var compType = $('img_comp_opt_view_mode').value;

            BS.Util.hide('img_comp_hint');
            $j('.statistics_images').css("display", "none");
            $('statistics_images_' + compType).style.display = (compType == "sxs" || compType == "diff") ? "flex" : "";
            
            if(compType == "diff") {
              var diffImage = BS.ImageCompResults.getResultFileName(artifact, "_diff");
              $('img_comp_difference').src = $('img_comp_difference_image').href = "/repository/download/${buildTypeExtID}/" + thisBuild.id + ":id/" + diffImage;
              $('img_comp_difference_image').innerText = "Diff image";
            }
            
            if(compType == "anim") {
              var animImage = BS.ImageCompResults.getResultFileName(artifact, "_animated", "webp");
              $('img_comp_anim_diff').src = "/repository/download/${buildTypeExtID}/" + thisBuild.id + ":id/" + animImage;
            } else {
              $('img_comp_left_' + compType).src = "/repository/download/${buildTypeExtID}/" + baselineId + ":id/" + artifact;
              $('img_comp_right_' + compType).src = "/repository/download/${buildTypeExtID}/" + thisBuild.id + ":id/" + artifact;

              $('img_comp_left_label_' + compType).href = "/viewLog.html?buildId=" + baselineId;
              $('img_comp_left_label_' + compType).innerText = "Baseline: #" + baselineNumber;
              $('img_comp_right_label_' + compType).href = "/viewLog.html?buildId=" + thisBuild.id;
              $('img_comp_right_label_' + compType).innerText = "This build: #" + thisBuild.number;

              if(compType == "slider" && BS.ImageCompResults.SliderInit == undefined) {
                BS.ImageCompResults.SliderInit = true;
                $j('.slider').slider();
              }
            }

            var images = $('statistics_images_' + compType).getElementsByTagName("img");
            for(var i = 0; i < images.length; i++) {
              var parent = images[i].parentElement;
              if(parent != undefined && parent.tagName == "A") {
                parent.href = images[i].src;
                parent.target = "_blank";
              }
            }
          }
          else
          {
              alert("Failed to look up baseline image (code " + transport.status + ")");
          }
        }
      });
    },

    getResultFileName(artifact, suffix, forceExt) {
      var presentExt = artifact.split('.').pop();
      var extension = (forceExt != undefined) ? forceExt : presentExt;
      var result = "${artifact_results_path}/" + artifact.substring(0, artifact.length - (presentExt.length + 1)) + suffix + "." + extension;

      //if it's pointing to an archive, then convert "image_comparisons/subfolder2/x.zip!/c_diff.png" to "image_comparisons/subfolder2/x_zip/c_diff.png"
      var mtch = result.match("(!$|!/)");
      if(mtch != undefined) {
        //remove '!'
        result = result.substring(0, mtch.index) + result.substring(mtch.index + 1);

        //swap '.' for '_'
        var archiveDot = result.lastIndexOf(".", mtch.index);
        if(archiveDot != -1) {
          result = result.substring(0, archiveDot) + "_" + result.substring(archiveDot + 1);
        }
      }

      return result;
    },

    keyDown(e) {
      if(e.target != document.body || BS.ImageCompResults.CurrentChartData == undefined) {
        return;
      }

      if(e.code == "ArrowLeft" && BS.ImageCompResults.SelectedIndex > 0) {
        BS.ImageCompResults.SelectedIndex--;
        BS.ImageCompResults.updateView();
      } else if(e.code == "ArrowRight" && BS.ImageCompResults.SelectedIndex < BS.ImageCompResults.CurrentChartData.length - 1) {
        BS.ImageCompResults.SelectedIndex++;
        BS.ImageCompResults.updateView();
      }
    },

    updateUrl() {

      if(window.history.pushState) {
        var thisUrl = new URL(document.location);
        thisUrl.searchParams.set('ic_count', $('img_comp_opt_count').value);
        thisUrl.searchParams.set('ic_artifact', $('img_comp_opt_artifact').value);
        thisUrl.searchParams.set('ic_metric', $('img_comp_opt_metric').value);
        thisUrl.searchParams.set('ic_view_mode', $('img_comp_opt_view_mode').value);

        window.history.pushState(null, null, thisUrl);
      }
    },

    createStatsGraph(onParentProject) {

      BS.Util.show('createGraphProgress');
      var artifact = $('img_comp_opt_artifact').value;
      var metric = $('img_comp_opt_metric').value;

      var source = onParentProject ? ('${buildTypeExtID}: ') : '';
      var title = 'Diff report for ' + source + artifact + ' (metric: ' + metric + ')';
      var seriesTitle = artifact + ' (' + metric + ')';
      var statistic = 'ic_' + artifact + '_' + metric;
      var sourceBuildType = (onParentProject ? ' sourceBuildTypeId="${buildTypeExtID}"' : '');

      var xml = [
        '<graph title="' + title + '" seriesTitle="' + seriesTitle + '" format="text">',
          '<valueType key="' + statistic + '" title="' + statistic  + '"' + sourceBuildType + '/>',
        '</graph>'
      ];

      BS.ajaxRequest(window['base_uri'] + '/editChart.html', {
        method: "POST",
        parameters: {
          'action': 'addChart',
          'projectId': '${projectIntId}',
          'buildTypeId': (onParentProject ? '' : '${buildTypeExtID}'),
          'chartGroup': (onParentProject ? 'project-graphs' : 'buildtype-graphs'),
          'newXml': xml.join('')
        },
        onComplete: function(transport) {
            BS.Util.hide('createGraphProgress');
            if(transport.status == 200)
            {
              document.location = onParentProject ? "${viewProjectStatsUrl}" : "${viewTypeStatsUrl}";
            }
            else
            {
              alert('Failed to create graph(code ' + transport.status + ')');
            }
        }
      });
    }
  };

  var params = new URLSearchParams(window.location.search);
  if(params.has("ic_count")) $('img_comp_opt_count').value = params.get("ic_count");
  BS.ImageCompResults.SetArtifact = params.get("ic_artifact");
  BS.ImageCompResults.SetMetric = params.get("ic_metric");
  if(params.has("ic_view_mode")) $('img_comp_opt_view_mode').value = params.get("ic_view_mode");
  BS.ImageCompResults.getData();
  
  document.addEventListener('keydown', BS.ImageCompResults.keyDown);

  window.addEventListener('popstate', function(e) {

    var newParams = new URL(document.location).searchParams;
    var count = newParams.get("ic_count");
    var artifact = newParams.get("ic_artifact");
    var metric = newParams.get("ic_metric");
    var view_mode = newParams.get("ic_view_mode");

    var doFullRefresh = $('img_comp_opt_count').value != count;
    var artChange = $('img_comp_opt_artifact').value != artifact;
    var metChange = $('img_comp_opt_metric').value != metric;
    var viewChange = $('img_comp_opt_view_mode').value != view_mode;

    $('img_comp_opt_count').value = count;
    $('img_comp_opt_artifact').value = artifact;
    $('img_comp_opt_metric').value = metric;
    $('img_comp_opt_view_mode').value = view_mode;

    if(doFullRefresh) {
      BS.ImageCompResults.getData();
    } else {
      if(artChange) {
        BS.ImageCompResults.updateArtifact();
      }
      if(metChange) {
        BS.ImageCompResults.updateGraph();
      }
      if(viewChange) {
        BS.ImageCompResults.updateView();
      }
    }
  });

</script>