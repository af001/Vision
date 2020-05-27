# Vision: Android computer vision implementation using custom models. 

This project consists of a template Android application and a set of Colab notebooks that can be used to build a custom Android computer vision appliation. Minor modifications to the Android application need to be made in Java to include the custom trained model, which can be generated using the provided set of Jupyter Notebooks. 

The Jupyter Notebooks are written in Python and can be opened in Google Colab or Anaconda. The notebooks are written to download datasets from Kaggle using the Kaggle API. This allows the user to have access to thousands of datasets with minimal changes to the original code. Due to the time it takes to train a custom model, the notebooks are configured to automatically save the models in Google Drive once training is complete. 

### Requirments
* Android Studio
* Python3 (Google Colab (cloud) --or-- Anaconda (local))

### Quick Start

```bash
# Git the project
git clone https://github.com/af001/vision.git

# Connect Android phone to adb
apt install adb-tools
adb devices

# Install debug Apk on Android Phone
gradle installDebug

# Open the app - alt: Click app icon on the phone
adb shell am start -n com.msds.vision/.MainActivity
````

### Custom Build

#### Train a custom model
The first thing you need is to identify the dataset you intend on using. The easists place to find datasets if through [Kaggle](https://www.kaggle.com). This is the preferred method due to the notebooks already being setup to ingetrate with the Kaggle API. Users could upload their datasets to Kaggle for easier long-term integration, if desired. 

Once a Kaggle dataset is identified, look at the dataset to see if it includes annotations. Annotations are normally created with tools such as [LabelImg](https://github.com/tzutalin/labelImg) and they are often used to create labels for new datasets. This means that each image contains a label as well as a bounding box with coordinates of the object in the image. If annotations are present, then use the [with_annotations](https://github.com/af001/vision/blob/master/notebooks/wit_annotations_dog-breeds.ipynb) template under notebooks. If the dataset is a directory containing sub-directories of images, then the sub-directories will act as the labels and the template notebook will be the [without_annotations]([with_annotations](https://github.com/af001/vision/blob/master/notebooks/without_annotations_bird-species.ipynb)) notebook. 

Open the notebook that you identified as a starting point, and click the "Open with Colab" link at the top. This will open the notebook in a user's personal Google Colab environment. The first thing to do once the notebook is important into a user's personal Colab environment is set the runtime. This can be done by clicking *Runtime -> Change runtime type* and setting *Hardware Accelerator -> GPU* then clicking *Ok.*

The next step is to change the dataset. The API command to download the dataset from Kaggle can be obtained by going to the Kaggle website and clicking the *Copy API command* button as seen in the screenshot below:

![Kaggle Dataset Download](https://i.ibb.co/hBRBrR0/Screen-Shot-2020-05-27-at-11-27-36-AM.png)

The three lines that need to be modified in the notebook are:
```bash
# Download the dataset
!kaggle datasets download -d jessicali9530/stanford-dogs-dataset

# Unzip the dataset
!unzip stanford-dogs-dataset.zip

# Cleanup
!rm stanford-dogs-dataset.zip
```
The next step is to obtain a Kaggle API key. This can be achieved on the Kaggle website under *My Account -> Create New API Token.*

![API Key Generation](https://i.ibb.co/1zd4qVK/Screen-Shot-2020-05-27-at-11-43-45-AM.png)

This will download a file called *kaggle.json* to you computer. Once complete, it is time to start trainig the custom model. To begin, click *Runtime -> Runa all.* As the Python libraries are imported, users will be asked to link their Colab account to their Google account. Click the link for the authorization code and paste the code into Google Colab. The next part will ask to upload you Kaggle API token. Click the upload button and select the kaggle.json file that was previously downloaded to your computer. 
