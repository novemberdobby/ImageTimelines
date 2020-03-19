<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="artifact_lookup_url" value="<%=Constants.ARTIFACT_LOOKUP_URL%>"/>
<c:set var="artifact_results_path" value="<%=Constants.ARTIFACTS_RESULT_PATH%>"/>

<script src="${resources}js/Chart.min.2_9_3.js"></script>
<script src="${resources}js/moment.min.2_24_0.js"></script>
<script src="${resources}js/imgslider.min.js"></script>
<link rel="stylesheet" type="text/css" href="${resources}css/imgslider.min.css">

<style type="text/css">
.icPermalink {
  background: url(${resources}images/permalink.png) 0 0 no-repeat;
  width: 16px;
  height: 16px;
  display: inline-block;
  vertical-align: text-bottom;
}
</style>

<forms:saving id="getImgDataProgress"/>
<div id="img_comp_options" style="display: none; border: 1px solid #868686; border-style: double; margin-bottom: 1em; margin-right: 0.5em; background: #e4e4e4;">
  <div style="padding: 0.25em; width: min-content; margin: 0.25em;">
    Builds
    <forms:saving id="getImgDataProgressBuilds" style="float: right;"/>
    <br>
    <select id="img_comp_opt_count"  onchange="BS.ImageCompResults.getData()">
      <option value="100" selected="true">100</option>
      <option value="200">200</option>
      <option value="500">500</option>
      <option value="1000">1000</option>
    </select>
  </div>

  <div style="padding: 0.25em; width: min-content; margin: 0.25em;">
    Artifact
    <br>
    <select id="img_comp_opt_artifact" onchange="BS.ImageCompResults.changeArtifact()"></select>
  </div>

  <div style="padding: 0.25em; width: min-content; margin: 0.25em;">
    Statistic
    <br>
    <select id="img_comp_opt_metric" onchange="BS.ImageCompResults.drawGraph()">
      <option value="-">-</option>
    </select>
  </div>

  <div style="padding: 0.25em; width: min-content; margin: 0.25em;">
    Permalink
    <br>
    <a class="icPermalink" href="#" onclick="BS.ImageCompResults.gotoPermaLink()"></a>
  </div>
  
  <div style="padding: 0.25em; width: min-content; margin: 0.25em; margin-left: auto; text-align: right;">
    View mode
    <br>
    <select id="img_comp_opt_view_mode" onchange="BS.ImageCompResults.updateView()">
      <option value="sxs">Side by side</option>
      <option value="slider">Diff slider</option>
      <option value="diff">Diff image</option>
      <option value="gif">Animated diff</option>
    </select>
  </div>
</div>

<div id="statistics_container" style="display: none;">

  <%-- old image on left, new image on right --%>
  <div id="statistics_images_sxs" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="width: 50%; border-right: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: left;">
        <a id="img_comp_left_label_sxs" target="_blank"></a>
      </div>
      <img id="img_comp_left_sxs" style="width: 100%; display: block;">
    </div>
    <div style="width: 50%; border-left: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: right;">
        <a id="img_comp_right_label_sxs" target="_blank"></a>
      </div>
      <img id="img_comp_right_sxs" style="width: 100%; display: block;">
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
    <div style="width: 33%; border-right: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: left;">
        <a id="img_comp_left_label_diff" target="_blank"></a>
      </div>
      <img id="img_comp_left_diff" style="width: 100%; display: block;">
    </div>

    <div style="width: 34%; border-right: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: center;">
        <a id="img_comp_difference_image" target="_blank"></a>
      </div>
      <img id="img_comp_difference" style="width: 100%; display: block;">
    </div>

    <div style="width: 33%; border-left: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: right;">
        <a id="img_comp_right_label_diff" target="_blank"></a>
      </div>
      <img id="img_comp_right_diff" style="width: 100%; display: block;">
    </div>
  </div>
  
  <%-- pre-generated animated gif of differences --%>
  <div id="statistics_images_gif" class="statistics_images" style="border: 2px solid black; display: none; height: min-content;">
    <div style="width: 100%; border-right: 1px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold; border-bottom: 2px solid black; text-align: left;">
      <img id="img_comp_gif_diff" style="width: 100%; display: block;">
      </div>
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
          var match = p.name.match("ic_([\\w\\.]+)_([\\w\\.]+)"); //ic_<artifactname>_<metricname>
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

      BS.Util.hide('getImgDataProgress');
      BS.Util.hide('getImgDataProgressBuilds');
      BS.Util.show('statistics_container');
      $('img_comp_options').style.display = "flex";
      BS.ImageCompResults.changeArtifact();
    },

    changeArtifact: function() {
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
      BS.ImageCompResults.drawGraph();
    },

    drawGraph: function() {
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
      var invLerp = function(a, b, c) { return (c - a) / (b - a); }
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
              
              if(compType == "gif") {
                var animImage = BS.ImageCompResults.getResultFileName(artifact, "_animated", "gif");
                $('img_comp_gif_diff').src = "/repository/download/${buildTypeExtID}/" + thisBuild.id + ":id/" + animImage;
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
            }
            else
            {
                alert("Failed to look up baseline image (code " + transport.status + ")");
            }
        }
      });
    },

    getResultFileName(artifact, suffix, forceExt) {
      var extension = (forceExt != undefined) ? forceExt : artifact.split('.').pop();
      return "${artifact_results_path}/" + artifact.substring(0, artifact.length - (extension.length + 1)) + suffix + "." + extension;
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

    gotoPermaLink() {
      var newUrl = "${viewTypeUrl}"
        + "&ic_count=" + $('img_comp_opt_count').value
        + "&ic_view_mode=" + $('img_comp_opt_view_mode').value
        + "&ic_artifact=" + $('img_comp_opt_artifact').value
        + "&ic_metric=" + $('img_comp_opt_metric').value;
      
      document.location = newUrl;
    }
  };
  
  var params = new URLSearchParams(window.location.search);
  if(params.has("ic_count")) $('img_comp_opt_count').value = params.get("ic_count");
  if(params.has("ic_view_mode")) $('img_comp_opt_view_mode').value = params.get("ic_view_mode");

  BS.ImageCompResults.SetArtifact = params.get("ic_artifact");
  BS.ImageCompResults.SetMetric = params.get("ic_metric");

  BS.ImageCompResults.getData();
  document.addEventListener('keydown', BS.ImageCompResults.keyDown);

</script>