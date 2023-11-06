1. Before deploying the website, we need to set up a Google Cloud Storage bucket using the same Google account used for `VRE1`. To do this, go to the [Google Cloud Console](https://console.cloud.google.com/).
2. At the top of the screen, you will see: "Start your free trial with a $300 credit. Don't worry, you won't be charged if credits run out." Click on "Start for free."
3. Set up your account with your personal details and credit card information.
4. Now, a Google Cloud Storage bucket will be created. Click on "Create" at the top of the screen [in the storage section](https://console.cloud.google.com/storage) of the Google Cloud Console. The bucket name should be "parcelas." Select the region for your bucket and click "Create." A prompt will appear asking if the bucket should be kept private, make sure "Apply public access prevention to this bucket" is checked.
5. To launch the website, we'll need to download two service account credentials from the Google Cloud Console. Follow these steps:
    1. At the top of the screen, click on the icon in the top left corner and select "IAM & Admin" and then "Service accounts."
    2. Click on "Create Service Account."
    3. In "Service Account ID," enter "developer" for the first account. Leave the other fields empty.
    4. In "Grant this service account access to project," select the roles "AI Platform Service Agent," "Editor," "Owner."
    5. Repeat steps 1-4 changing the ID to "storage" and the role to "Owner."
    6. In the [main service accounts menu](https://console.cloud.google.com/iam-admin/serviceaccounts), click on each of the created service accounts and then "Keys", "Add Key", and "Create new key in JSON format".
    7. Rename the JSON file for the "developer" service account to "gee-cli-key.json" and the "storage" service account to "key.json."
    8. Move both keys to a folder named `credentials/` in the cloned repository.
6. The last requirement to deploy the website is to have a service implementing OAuth 2.0 in the Python script `oauth.py`. In our case, we have used [Casdoor](https://casdoor.org/docs/how-to-connect/oauth/). If using "Casdoor," modify the ".env" file with your Casdoor account details (`CASDOOR_CLIENT_ID` and `CASDOOR_CLIENT_SECRET`). If using another service, modify the ".env" file (provided values) and "config.py" (default values) with your service data.
7. Finally, we'll build and run the Docker image that runs the website with the following commands from the main directory of the cloned repository:
    ```
    docker build -t app_visualizationgee:1.0.0 -f visualizationGEE.dockerfile .
    ```
    ```
    docker run -p 8083:8083 -v $PWD/credentials:/credentials app_visualizationgee:1.0.0
    ```