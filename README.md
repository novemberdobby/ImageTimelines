# Image Timelines - TeamCity plugin for comparing image output of tests

[![Demo video](/images/demo_preview.png)](http://www.youtube.com/watch?v=c7v1fSYcVI8 "Demo")
The plugin was written with game development in mind, however it's suitable for any process outputting images that are expected to be near-perfectly consistent.

## Usage
1. Install the plugin from Administration > Plugins List
2. Add the "Image timelines" build feature to any build configuration or template, and set options as desired:
![feature](/images/feature.png "Build feature")

After all steps/runners of a build have completed, the agent will attempt to compare the specified images (if they've been published as artifacts) to a previous build.

#### Comparison modes
*Last build*: use the last finished build, whether it succeeded or not.
*Last build with tag*: find the last build with the specified tag and use it as the baseline (also disregards success).
*Build with ID*: use a specific build. Note that this uses the build ID, not the number, which can be found in the URL of any build results page.

#### Comparison methods/metrics
One or more metrics, for example peak signal to noise ratio, can be generated from each comparison. See the [ImageMagick documentation](https://imagemagick.org/script/command-line-options.php#metric) for explanations of each metric.

#### Advanced options
![feature](/images/feature_adv.png "Advanced settings")
*Flicker*: enables the creation of .webp images for the "animated diff" view.
*Fail build on error*: marks the build as failed if the agent is unable to download artifacts or compare images.

### Results
The plugin creates an "image_comparisons" folder in the artifacts of each run:
![artifacts](/images/artifacts.png "Artifacts")
Once these have started appearing, navigate to the timelines page from any finished build:
![tab_build_type](/images/tab_build_type.png "Build type tab")
or from the build type page:
![tab_build_instance](/images/tab_build_instance.png "Build tab")

### Timeline view
This is the main plugin page for viewing comparisons:
![timeline](/images/timeline.png "Timeline")

1. Left toolbar:
   * Builds: how many past builds to search through for relevant data
   * Artifact: image being compared
   * Statistic: which comparison metric to display
   * View mode:
      * Side by side: display the old & new images
      * Diff slider: "overlay" the two images with a movable slider
      * Diff image: display the old, new, and generated diff images (as shown above)
      * Animated diff: display a .webp animation showing the old & new images
      * **Note: images may be missing if artifacts were cleaned up or settings were changed, e.g. animated diff generation was disabled or comparison list was modified. Nothing is generated retroactively!**
   * New window: open a new dedicated full-screen tab to more closely inspect images
2. Right toolbar:
   * Graph (build type/parent project): create a graph on the build type or project's statistics page showing the currently selected image/metric results
   * Set new baseline: update the build feature so that the current (normally right-hand side) build becomes the new baseline image
3. Diff view:
   * Column headers show relevant build numbers and can be clicked to view
   * Clicking any image in the diff view opens it in a new tab, except in "diff slider" mode
4. Y-axis: result of the comparison operation for each build
5. Data: X-axis labels denote build numbers. Hover over any bar to show details, a grey bar marks the current comparison. Use the left/right arrow keys to move around.

Once you've changed any setting on the toolbar, copy the page URL for a direct link to the current view.

### Statistics
Each numerical comparison result is stored as a TeamCity statistic on each build:
![statistics](/images/statistics.png "Statistics")
**Note**: while this page only displays numbers to two decimal places, a more accurate value is stored and this is shown in statistic graphs & the build log.

To fail a build when a large difference is detected, first [add the relevant statistic to TC](https://www.jetbrains.com/help/teamcity/2020.2/build-failure-conditions.html#Adding+custom+build+metric) (which takes effect immediately) then add a new failure condition:
![stat_compare](/images/stat_compare.png "Stat comparison")
Be sure to also update the reference build above when you change it in the plugin's build feature:
![stat_compare_types](/images/stat_compare_types.png "Stat comparison types")

## Considerations
* When using last build/tag mode, be cautious of allowing multiple builds to run at once as the results may not be as expected.
* Comparisons using several metrics can be extremely slow to compute. Consider using multiple metrics starting out in order to determine the most relevant for your use case, then narrow down the list.
* The plugin doesn't generate thumbnails at any point. This means that the timeline view displays raw images, which may cause performance issues if they're very large.

## Building
This plugin is built with Maven. To compile it, run the following in the root folder:
```
mvn package
```
Version 7.0.10-3 of ImageMagick is included in the agent component, follow these steps to update it:
1. Download the latest portable binary distributions [here](https://imagemagick.org/script/download.php).
2. Overwrite the files in **/imageComp-agent/ImageMagick**. Many of the Windows executables are unused and can be deleted to save space, check the existing files to see which ones.
3. Compile the plugin.

## Limitations
Linux agents are not fully supported, your mileage may vary. You may need to install cwebp.