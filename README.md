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
The first thing you need is to identify the dataset you intend on using. The easists place to find datasets if through [Kaggle](https://www.kaggle.com).

