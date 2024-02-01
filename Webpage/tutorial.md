# Tutorial for Using Smartfood's VRE1 from the Webpage

## Context
We have deployed a provisional version of a Smartfood webpage on [Visualization-GEE - Portal (uma.es)](https://khaos.uma.es/visualization-GEE/). The code and deployment manual are completed and available at [SMARTFOOD/Webpage at main · KhaosResearch/SMARTFOOD (github.com)](https://github.com/KhaosResearch/SMARTFOOD/tree/main/Webpage). The page is used for user authentication and primarily as a user interface to access VRE1 and manage Shapefile data (tabular data with associated geometries) used in applications developed in Google Earth Engine. Below, we specify how to access the VRE1 applications (developed in GEE) from the webpage using a test account that has Shapefile data uploaded. If desired, users can also create an account and upload Shapefile data to further test the applications.

## Steps to Access the Applications:
1. Visit  [Visualization-GEE - Portal (uma.es)](https://khaos.uma.es/visualization-GEE/) and click "Login" in the top right corner.
2. The test account is user `sm`, password `123456789`. If you wish, you can create a new account, but you will need to upload a Shapefile to test the applications.
3. Once logged in, click on “Available services” to display links to different VREs.
4. To access the VRE1 section, click on “Near and remote sensing of agriculture: Sensor integration and connectivity”.
5. Inside, you will find three different actions you can perform:
   a. “Upload shapefile” is used to upload “.shp” files and their derivatives (all compressed into a “.zip”) to the cloud, so that the GEE applications can access them, and thereby, the user.
   b. In “Data management”, you can see the current status of Shapefile upload requests, as well as those currently uploaded by the identified user. It is also possible to delete uploaded files from the cloud.
   c. In “Applications”, you can access all applications uploaded in GEE.
6. If using the test account, you can directly go to “Applications” to test the applications. If you have created an account, click on “Upload shapefile” to upload a Shapefile (must be compressed in zip format).
7. Once we have accessed the applications, we will see that there are applications for analyzing data from three different types of satellite images: “Sentinel-1”, “Sentinel-2”, and “Sentinel-5P”. Moreover, for each satellite, there are two applications. The choice of application depends on the data you want to analyze. In the “SigPac” application, we can analyze SIGPAC plots (Geographic Information System of Agricultural Parcels) in the Andalusia region. In the “Column” application, we can analyze plots uploaded to the application categorized by a column that appears in the tabular data associated with the geometries.
8. Before entering an application, copy the ID that appears in the top center of the screen. For the test account, it would be `d3ca-53e1-9b59-761c`.
9. Let's enter an application of each type to test it. For example, click on “SigPac S2” to analyze SIGPAC plot data in Andalusia using Sentinel-2 data.
10. We will be redirected to GEE. The first step in all applications is to paste the ID we copied into the text field in the top left corner (“Add user code”). Press “enter” on the keyboard to confirm, and a new menu will open with the Shapefiles uploaded under the ID field.
11. Select the Shapefiles you want to analyze and click “Confirm selection”. Then, the map will center on the area of the selected Shapefile.
12. Click on “Select plot”, which will open a menu with a “Point” button. You must click this button to select the SIGPAC plots you want to analyze. You can add as many plots as desired. Once all are added, click on “confirm selection” to finalize the plot selection.
13. To perform the analysis of the selected plots with satellite images, select a start date, an end date, and a maximum cloud percentage in the left menu. The analysis will take the available satellite images in that date range that meet the specified maximum cloud percentage filter.
14. The last step is to select the bands or indices to be considered for the analysis. For each selected band or index, a time series will be created with the values in each plot. Once selected, click on “Execute”.
15. After clicking the button, a graph will appear on the left for each selected band or index. For each selected plot, a line will appear on each graph indicating the value for each band or index that each plot has had in the indicated range. 
16. With steps 10 to 16, we have tested the "SigPac S2" application. These steps can be applied to the "SigPac S1" and "SigPac S5" applications, the only difference being the type of satellite images, but the user interface and usage are the same.
17. Let's now test a "Column" type application. On the website, click the "Column S2" application. The "Column" type applications require that the uploaded data have a categorical column, as they perform the same analysis as in the "SigPac" applications but grouped by the indicated column, instead of by SIGPAC plot.
18. For using the application, you can follow steps 10-16 again. The only difference is step 12. Instead of selecting SIGPAC plots, we will need to fill in the field "Add column used to group the farms". With the test account, we can select "finca2" and use the column "CD_USO".
19. Once we have followed all the steps, we will see that the analysis has been performed for the entire Shapefile, but there is a line in each graph of the analysis for each category that appears in the column "CD_USO".
