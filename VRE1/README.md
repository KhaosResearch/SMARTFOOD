# Pasos para desplegar todo el sistema.

1. Crear una cuenta de Gmail (o elegir una existente), que se usará para Google Storage (necesita tarjeta de crédito).
2. Entra en [Earth Engine code editor](https://code.earthengine.google.com/) usando la misma cuenta de Gmail.
3. Pulsa "I want to register a new project", que aparecerá en la pantalla si es la primera vez que entras en el code editor. Si no aparece, pulsa en el icono de la esquina superior derecha y selecciona "Register a new cloud project".
4. Pulsa "Register a Noncommercial or Commercial Cloud project"
5. Pulsa "Unpaid usage: Non-profits, education, government research, training, media." y seleccione un "proyect type" que encaje en tu organización.
6. Pulsa "Create a new Google cloud project" y nombre el proyecto rellenando el campo único "Project-ID". Si aparece un prompt al aceptar que dice "You must accept the Cloud Terms of Service before a Cloud Project can be created.", pulsa en el enlace que hay en el prompt y acepta los términos de servicio.
7. Al confirmar, se te redirigirá al Earth Engine code editor.
8. En el code editor, pulsa en "New" y selecciona "Repository". Tendrás que confirmar un identificador de nombre de usuario si es la primera vez que creas uno, y un nombre de repositorio. Ten en cuenta que el nombre del usuario aparecerá en la URL que Google crea para las aplicaciones que se desarrollen en el code editor. El nombre del repositorio no tiene tanta importancia.
9.  Clona el repositorio de GitHub [Khaos-Research/SMARTFOOD](https://github.com/KhaosResearch/SMARTFOOD) usando LFS para descargar los shapefile necesarios.
10. Ejecuta en una terminal el siguiente comando desde el directorio raiz del repositorio para sustituir en los scripts desarrollados los identificadores de proyecto por los tuyos. Ten en cuenta que en el comando debes sustituir "YOUR-PROJECT-ID" por su ID de proyecto seleccionado anteriormente:

    ```
    find ./VRE1 -type f -name "*.js" -exec sed -i 's/<ID-PROYECTO>/YOUR-PROJECT-ID/g' {} +
    ```

11. Asegurarse de que se han descargado los tres shapefile (comprimidos en .zip), que se encuentra en la carpeta `VRE1/shapefiles/`. En esta carpeta se encuentran dos archivos del SIGPAC [[LICENCIA]](https://www.juntadeandalucia.es/organismos/agriculturapescaaguaydesarrollorural/servicios/sigpac/visor/paginas/sigpac-descarga-informacion-geografica-shapes-provincias.html) a nivel de parcela y polígono, y los límites de las provincias de Andalucía.
12. Ahora tenemos que subir los archivos del SIGPAC y los límites de andalucía al proyecto de Google Cloud. En la parte izquierda del editor, pulsar "Assets". Pulsar "New" y "Folder". Crear la carpeta con el nombre "andalucia" en minúscula y sin tilde. Valide que aparece en pantalla al crearla `projects/<ID_PROYECTO>/assets/andalucia`.
13. Para cada uno de los tres archivos .zip descargados (`limites.zip`, `andalucia_nv3.zip` y `andalucia_nv5.zip`), pulse de nuevo en "New", pero esta vez en "Shape files (.shp, .shx, .dbg, .prj or .zip)". Se abrirá un recuadro en el que en "Source files" se seleccionará uno de los archivos ".zip", que contienen los shapefiles. En "Asset ID", deberá añadir los siguientes identificadores (cada uno respectivo con el nombre de su archivo .zip):
    - `projects/<ID_PROYECTO>/assets/andalucia/limites` seleccionando el archivo `limites.zip` en source files.
    - `projects/<ID_PROYECTO>/assets/andalucia/andalucia_nv3` seleccionando el archivo `andalucia_nv3.zip` en source files.
    - `projects/<ID_PROYECTO>/assets/andalucia/andalucia_nv5` seleccionando el archivo `andalucia_nv5.zip` en source files.
14. Espere a que se suban todos los archivos. Puede acceder al [task manager](https://code.earthengine.google.com/tasks) para ver el estado de cada una. Tenga en cuenta que puede tardar hasta una hora en subirse todo debido al gran tamaño de los archivos. Esta acción solo será necesaria realizarla una vez.
15. Se han desarrollado 6 aplicaciones en GEE, 2 para cada satélite Sentinel-1 (S1), Sentinel-2 (S2) y Sentinel-5p (S5P). Para crear aplicaciones para las 6 aplicaciones desarrolladas (archivos `.js` del directorio base de la rama main), se tienen que hacer los siguientes pasos para cada una de ellas:  
    1. En el code editor, pulsa en "New" y selecciona "File". 
    2. Cree el archivo con el mismo nombre que el archivo `.js` dentro del repositorio creado anteriormente. Por ejemplo, el archivo `Smartfood_GEE_column_S1.js` en `users/<ID_USUARIO>/<ID_REPOSITORIO>/Smartfood_GEE_column_S1>`.
    3. Copie el contenido del archivo .js en el archivo creado en el code editor y pulse `save`.
    4. Pulse `run` para validar el funcionamiento de la aplicación. Si todo va bien, se ejecutará en modo desarrollo. Tenga en cuenta que algunas aplicaciones necesitan tener archivos subidos y no se podrá comprobar su funcionamiento hasta que se hagan los siguientes pasos. Al final del despliegue, se volverán a probar con archivos subidos.
    5. Pulse en "Apps" y a continuación, en "New App". Seleccione su proyecto cuando pregunte por "Editing access", de forma que la url de la app será `<PROJECT_ID>.projects.earthengine.app`. El nombre de la app debe ser el mismo que el del fichero `.js` del repositorio. Por ejemplo, el nombre de la app para el archivo `Smartfood_GEE_column_S1.js` debe ser `Smartfood_GEE_column_S1`. De `App source code` seleccione `repository script path`, y rellene con el nombre del script en el `Earth Engine code editor`. Finalmente, pulse en `Publish`.
16. La parte del VRE1 está completada, siga las instrucciones en la carpeta `Webpage/` para desplegar la página web general de Smartfood, que permite subir archivos y probar las aplicaciones.


- En `apps.html` cambiar la referencia a las apps e.g. `https://vregeesmartfood.users.earthengine.app/view/usoagrario-s1`