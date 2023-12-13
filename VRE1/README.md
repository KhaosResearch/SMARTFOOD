# Steps to deploy the entire system.

1. Create a Gmail account (or use an existing one) to be used for Google Storage (requires credit card).
2. Go to the [Earth Engine code editor](https://code.earthengine.google.com/) using the same Gmail account.
3. Click "I want to register a new project", which will appear on the screen if it's your first time entering the code editor. If not, click on the icon in the top right corner and select "Register a new cloud project."
4. Click "Register a Noncommercial or Commercial Cloud project."
5. Click "Unpaid usage: Non-profits, education, government research, training, media." and select a "project type" that fits your organization.
6. Click "Create a new Google cloud project" and name the project by filling in the unique "Project-ID" field. If prompted to accept the message "You must accept the Cloud Terms of Service before a Cloud Project can be created", click the link in the prompt and accept the terms of service.
7. Upon confirmation, you will be redirected to the Earth Engine code editor.
8. In the code editor, click on "New" and select "Repository." You'll need to confirm a username identifier if you're creating one for the first time and a repository name. Note that either the username or project ID will appear in the URL that Google creates for applications developed in the code editor. The repository name is not that relevant.
9. Clone the GitHub repository [Khaos-Research/SMARTFOOD](https://github.com/KhaosResearch/SMARTFOOD) using LFS to download the necessary shapefiles.

    ```
    apt install git-lfs
    git lfs clone https://github.com/KhaosResearch/SMARTFOOD.git
    ```
    
10. Run the following command in a terminal from the root directory of the repository to replace project identifiers in the developed scripts with your own. Please replace "YOUR-PROJECT-ID" with your selected project ID in the next command:

    ```
    find ./VRE1 -type f -name "*.js" -exec sed -i 's/<PROJECT-ID>/YOUR-PROJECT-ID/g' {} +
    ```

11. Ensure that the four shapefiles (compressed in .zip) have been downloaded (they will be provided by Khaos). These files are: two SIGPAC files [[LICENSE ref]](https://www.juntadeandalucia.es/organismos/agriculturapescaaguaydesarrollorural/servicios/sigpac/visor/paginas/sigpac-descarga-informacion-geografica-shapes-provincias.html) at the parcel and polygon levels, the boundaries of the provinces of Andalusia, and its ecological sites.
12. Now, upload the SIGPAC files and Andalusia boundaries to the Google Cloud project. On the left side of the editor, click on "Assets." Click "New" and "Folder." Create the folder with the name "andalucia" in lowercase and without accents. Verify that it appears on the screen upon creation `projects/<PROJECT-ID>/assets/andalucia`.
13. For each of the four .zip files (`sitios.zip`, `limites.zip`, `andalucia_nv3.zip`, and `andalucia_nv5.zip`), click again on "New," but this time select "Shape files (.shp, .shx, .dbg, .prj, or .zip)." A dialog will open where you will select one of the ".zip" files containing the shapefiles in "Source files." In "Asset ID," add the following identifiers (each respective to its .zip file name):
    - `projects/<PROJECT-ID>/assets/andalucia/limites` selecting the `limites.zip` file in source files.
    - `projects/<PROJECT-ID>/assets/andalucia/andalucia_nv3` selecting the `andalucia_nv3.zip` file in source files.
    - `projects/<PROJECT-ID>/assets/andalucia/andalucia_nv5` selecting the `andalucia_nv5.zip` file in source files.
    - `projects/<PROJECT-ID>/assets/andalucia/sitios` selecting the `sitios.zip` file in source files.
14. Wait for all the files to upload. You can check the status of each upload in the [task manager](https://code.earthengine.google.com/tasks). Note that it may take up to four hours to upload everything due to the large file size. This action only needs to be done once.
15. Six applications have been developed in GEE, 2 for each Sentinel-1 (S1), Sentinel-2 (S2), and Sentinel-5p (S5P) satellite. To create applications for the 6 developed apps (`.js` files in the main branch base directory), follow these steps for each of them:
    1. In the code editor, click on "New" and select "File."
    2. Create the file with the same name as the `.js` file inside the previously created repository. For example, the file `Smartfood_GEE_column_S1.js` in `users/<USER-ID>/<REPOSITORY-ID>/Smartfood_GEE_column_S1>`.
    3. Copy the content of the .js file into the created file in the code editor and click `save`.
    4. Click `run` to validate the application's functionality. If everything is fine, it will run in development mode. Note that some applications need to have files uploaded, and their functionality cannot be checked until the following steps are completed. At the end of the deployment, they will be tested again with uploaded files.
    5. Click on "Apps" and then "New App." Select your project when asked for "Editing access," so the app's URL will be `<PROJECT-ID>.projects.earthengine.app`. The app's name must be the same as the `.js` file in the repository. For example, the app's name for the `Smartfood_GEE_column_S1.js` file should be `Smartfood_GEE_column_S1`. From `App source code`, select `repository script path` and fill in with the script's name in the `Earth Engine code editor`. Finally, click on `Publish`.
16. The VRE1 part is completed. Follow the instructions in the `Webpage/` folder to deploy the Smartfood general website, which allows uploading files and testing the applications.
