<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="artifact_lookup_url" value="<%=Constants.ARTIFACT_LOOKUP_URL%>"/>

<script src="${resources}js/Chart.min.2_9_3.js"></script>
<script src="${resources}js/moment.min.2_24_0.js"></script>
<script src="${resources}js/imgslider.min.js"></script>
<link rel="stylesheet" type="text/css" href="${resources}css/imgslider.min.css">

<forms:saving id="getImgDataProgress"/>

<%-- TODO: permalink button that goes straight to a provided artifact/stat/count --%>
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
    <select id="img_comp_opt_stats" onchange="BS.ImageCompResults.drawGraph()">
      <option value="-">-</option>
    </select>
  </div>
  
  <div style="padding: 0.25em; width: min-content; margin: 0.25em; margin-left: auto; text-align: right;">
    View mode
    <br>
    <select id="img_comp_opt_view_mode" onchange="BS.ImageCompResults.updateView()">
      <option value="sxs">Side by side</option>
      <option value="diff">Diff slider</option> <%-- TODO --%>
    </select>
  </div>
</div>

<%-- TODO: _diff image mode (check it exists) --%>
<div id="statistics_container" style="display: none;">

  <div id="statistics_images_sxs" style="border: 2px solid black; display: none; height: min-content;">
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

  <div id="statistics_images_diff" style="border: 2px solid black; display: none; height: min-content;">
    <div style="display: flex; border-bottom: 2px solid black;">
      <div style="overflow: hidden; padding: 4px; font-weight: bold;">
        <a id="img_comp_left_label_diff" target="_blank"></a>
      </div>
      <div style="overflow: hidden; padding: 4px; font-weight: bold; margin-left: auto;">
        <a id="img_comp_right_label_diff" target="_blank"></a>
      </div>
    </div>

    <div class="slider">
      <div class="slider responsive">
        <div class="left image">
          <img id="img_comp_left_diff" style="display:block;"/>
        </div>
        <div class="right image">
          <img id="img_comp_right_diff" style="display:block;"/>
        </div>
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
      var ddStats = $('img_comp_opt_stats');
      var oldStat = ddStats.value;
      ddStats.innerHTML = "";

      const targetArtifact = Artifacts[ddArtifacts.value];
      if(targetArtifact != undefined) {
        for(var stat in targetArtifact) {
          ddStats.options.add(new Option(stat, stat))
        }
      }
      
      var newStat = ddStats.value;
      ddStats.value = oldStat;
      if(ddStats.value == "") {
        ddStats.value = newStat;
      }

      //show initial graph
      BS.ImageCompResults.drawGraph();
    },

    drawGraph: function() {
      BS.Util.hide('statistics_images_sxs');
      BS.Util.hide('statistics_images_diff');
      BS.Util.show('img_comp_hint');
      var targetArtifact = $('img_comp_opt_artifact').value;
      var targetStat = $('img_comp_opt_stats').value;
      
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
      const target = Artifacts[targetArtifact][targetStat];
      BS.ImageCompResults.CurrentChartData = target;
      const values = target.map(d => d.value);
      var targetMin = 0; //TODO: is this OK to assume for all metrics?
      var targetMax = values.reduce((a, b) => Math.max(a, b));

      var lerp = function(a, b, c) { return a + (b - a) * c; }
      var invLerp = function(a, b, c) { return (c - a) / (b - a); }
      var colourLerp = function(a, b, c) { return "rgba(" + lerp(a[0], b[0], c) + "," + lerp(a[1], b[1], c) + "," + lerp(a[2], b[2], c) + "," + lerp(a[3], b[3], c) + ")" }

      BS.ImageCompResults.Chart.data.labels = target.map(d => d.number);
      BS.ImageCompResults.Chart.data.datasets = [{
          label: targetStat,
          data: values,
          backgroundColor: context => {
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
    },

    updateView: function() {
      if(BS.ImageCompResults.SelectedIndex == -1 || BS.ImageCompResults.CurrentChartData == undefined) {
        return;
      }
      
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
              $('statistics_images_sxs').style.display = "none";
              $('statistics_images_diff').style.display = "none";
              $('statistics_images_' + compType).style.display = compType == "diff" ? "" : "flex";
              
              $('img_comp_left_' + compType).src = "/repository/download/${buildTypeExtID}/" + baselineId + ":id/" + artifact;
              $('img_comp_left_label_' + compType).href = "/viewLog.html?buildId=" + baselineId;
              $('img_comp_left_label_' + compType).innerText = "Baseline: #" + baselineNumber;

              $('img_comp_right_' + compType).src = "/repository/download/${buildTypeExtID}/" + thisBuild.id + ":id/" + artifact;
              $('img_comp_right_label_' + compType).href = "/viewLog.html?buildId=" + thisBuild.id;
              $('img_comp_right_label_' + compType).innerText = "This build: #" + thisBuild.number;

              if(compType == "diff" && BS.ImageCompResults.SliderInit == undefined) {
                BS.ImageCompResults.SliderInit = true;
                $j('.slider').slider();
              }
            }
            else
            {
                alert("Failed to look up baseline image (code " + transport.status + ")");
            }
        }
      });
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
    }
  };

  BS.ImageCompResults.getData();
  document.addEventListener('keydown', BS.ImageCompResults.keyDown);
  
</script>