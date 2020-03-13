<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ page import="novemberdobby.teamcity.imageComp.common.Constants" %>

<c:set var="num_builds" value="<%=Constants.GRAPH_NUM_BUILDS%>"/>
<forms:saving id="getImgDataProgress"/>

<div id="img_comp_options" style="display: none;">
  <div style="padding: 0.25em; border: 1px solid #868686; width: min-content; margin: 0.25em; background: #e0e0e0;">
  Artifact
  <br>
  <select id="img_comp_artifact" onchange="BS.ImageCompResults.changeArtifact()"></select>
  </div>

  <div style="padding: 0.25em; border: 1px solid #868686; width: min-content; margin: 0.25em; background: #e0e0e0;">
  Statistic
  <br>
  <select id="img_comp_stats"  onchange="BS.ImageCompResults.drawGraph()">
    <option value="-">-</option>
  </select>
  </div>
  
  <%--TODO show X builds <div style="padding: 0.25em; border: 1px solid #868686; width: min-content; margin: 0.25em; background: #e0e0e0; float: left;">
  Statistic
  <br>
  <select id="img_comp_stats"  onchange="BS.ImageCompResults.drawGraph()">
    <option value="-">-</option>
  </select>
  </div>--%>
</div>

<script src="${resources}/Chart.min.2_9_3.js"></script>

<div id="statistics_container" style="display: none;">
  <div id="statistics_images" style="height: 50em; background: lightgray; border: 1px solid black;">
  </div>
  <div style="padding-top: 0.5em; padding-bottom: 1em;">Note: graph extents vary. <span style="color:#ff0000"><strong>Red</strong></span> bars show the highest values in the <strong>currently</strong> visible set.</div>
  <canvas id="stats_chart" height="70em"></canvas>
</div>

<script type="text/javascript">

  BS.ImageCompResults = {

    Artifacts: {},

    getData: function() {
      BS.Util.show('getImgDataProgress');
      $j.getJSON(base_uri + '/app/rest/builds?locator=buildType(internalId:${buildTypeIntID}),count:${num_builds}&fields=build(id,number,status,buildType(id,name,projectName),statistics(property(name,value)))',
        function(data) {
            BS.ImageCompResults.parseData(data);
        }
      )
    },

    parseData: function(buildData) {
      Artifacts = {}; //TODO show how many builds are being displayed
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

            Artifacts[name][stat].push({ build: build.number, value: p.value });
          }
        });
      }

      //fill artifact dropdown
      var ddArtifacts = $('img_comp_artifact');
      ddArtifacts.innerHTML = "";
      for(var art in Artifacts) {
        ddArtifacts.options.add(new Option(art, art))
      }

      BS.Util.hide('getImgDataProgress');
      BS.Util.show('statistics_container');
      $('img_comp_options').style.display = "flex";
      BS.ImageCompResults.changeArtifact();
    },

    changeArtifact: function() {
      //fill stats dropdown based on selected artifact
      var ddArtifacts = $('img_comp_artifact');
      var ddStats = $('img_comp_stats');
      ddStats.innerHTML = "";

      const targetArtifact = Artifacts[ddArtifacts.value];
      if(targetArtifact != undefined) {
        for(var stat in targetArtifact) {
          ddStats.options.add(new Option(stat, stat))
        }
      }

      //show initial graph
      BS.ImageCompResults.drawGraph();
    },

    drawGraph: function() {
      var targetArtifact = $('img_comp_artifact').value;
      var targetStat = $('img_comp_stats').value;
      
      //TODO: support showing multiple sets e.g. psnr + dssim etc. will need to reintroduce category spacing & make colours show metric
      //set up chart
      var context = document.getElementById('stats_chart').getContext('2d');
      if(BS.ImageCompResults.Chart == undefined) {
        BS.ImageCompResults.Chart = new Chart(context, {
          type: 'bar',
          options: {
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
            onClick: function(event, items) {
              if(items.length == 1) {
                alert("Item " + items[0]._index + " clicked");
              }
            }
          }
        });
      }

      BS.ImageCompResults.Chart.data.labels.clear();
      BS.ImageCompResults.Chart.data.datasets.clear();

      //collect data
      const target = Artifacts[targetArtifact][targetStat];
      const values = target.map(d => d.value);
      var targetMin = 0; //TODO: is this OK to assume for all metrics?
      var targetMax = values.reduce((a, b) => Math.max(a, b));

      var lerp = function(a, b, c) { return a + (b - a) * c; }
      var invLerp = function(a, b, c) { return (c - a) / (b - a); }
      var colourLerp = function(a, b, c) { return "rgba(" + lerp(a[0], b[0], c) + "," + lerp(a[1], b[1], c) + "," + lerp(a[2], b[2], c) + "," + lerp(a[3], b[3], c) + ")" }

      //TODO can we show dates in the tooltips?
      BS.ImageCompResults.Chart.data.labels = target.map(d => d.build);
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
          hoverBackgroundColor: "rgb(128, 128, 128, 255)",
          categoryPercentage: 1,
          barPercentage: 1.05, //overlap a little so there's no gap. looks a bit silly when there are <10 data points but it's not terrible
          minBarLength: 2,
      }];
      
      BS.ImageCompResults.Chart.update();
    }
  };

  BS.ImageCompResults.getData();
  
</script>