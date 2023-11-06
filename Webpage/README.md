1. Antes de levantar la web, necesitamos configurar un bucket de Google Cloud Storage con la misma cuenta de Google usada para el `VRE1`. Para ello, vaya a la [consola de Google Cloud](https://console.cloud.google.com/)
2. En la parte superior de la pantalla, verá: "Comienza tu prueba gratuita con un crédito de $300. No te preocupes, no se te cobrará si se acaban los créditos.", pulse en "Comenzar gratis". 
3. Configure su cuenta con sus datos personales y su tarjeta de crédito.
4. Ahora se va a crear un bucket de Google Cloud Storage. Pulse en "Crear" en la parte superior de la pantalla [en la parte de almacenamiento](https://console.cloud.google.com/storage) de la consola de Google Cloud. El nombre del bucket debe ser "parcelas". Seleccione la región de su bucket y pulse "Crear". Saltará un aviso que preguntará si se debe mantener privado el bucket, aseguresé de que "Aplicar la prevención de acceso público a este bucket" está marcado.
5. Para lanzar la página web, necesitaremos descargar dos credenciales para cuentas de servicio desde la consola de Google Cloud. Para ello, siga los siguientes pasos:
    1.  En la parte superior de la pantalla, pulse en el icono de la esquina superior izquierda y seleccione "IAM y administración" y luego "Cuentas de servicio".
    2.  Pulse "crear cuentas de servicio".
    3.  En "ID de la cuenta de servicio" ponga en la primera cuenta "developer". El resto de campos vacíos.
    4.  En "Otorga a esta cuenta de servicio acceso al proyecto" seleccione los roles "Agente de servicio de AI Platform", "Editor", "Propietario".
    5.  Repita los pasos 1-4 cambiando el ID por "storage" y el rol "propietario".
    6.  En el [menú principal de las cuentas de servicio](https://console.cloud.google.com/iam-admin/serviceaccounts), pulse en cada una de las cuentas de servicio creadas y luego en "Claves", "Agregar clave" y "Crear clave nueva en formato JSON".
    7.  Renombre el archivo JSON de la cuenta de servicio "developer" a "gee-cli-key.json" y la cuenta de servicio "storage" a "key.json".
    8.  Mueva ambas claves a una carpeta llamada `credentials/` en el repositorio clonado.
6. El último requisito para levantar la página web es disponer de un servicio que implemente Oauth 2.0 en el script de Python `oauth.py`. En nuestro caso, se ha usado [Casdoor](https://casdoor.org/docs/how-to-connect/oauth/). En el caso de usar "Casdoor" también, modificar el archivo ".env" con los datos de su cuenta de Casdoor (`CASDOOR_CLIENT_ID` y `CASDOOR_CLIENT_SECRET`). Si se usa otro servicio, modificar el archivo ".env" (valores proporcionados) y "config.py" (valores por defecto) con los datos de su servicio.
7. Finalmente, vamos a construir y ejecutar la imagen docker que ejecuta la página web con los siguientes comandos desde el directorio principal del repositorio clonado:
    ```
    docker build -t app_visualizationgee:1.0.0 -f visualizationGEE.dockerfile .
    ```
    ```
    docker run -p 8083:8083 -v $PWD/credentials:/credentials app_visualizationgee:1.0.0
    ```