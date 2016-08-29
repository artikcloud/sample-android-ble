# ARTIK Cloud sample app AKCnBLE

The sample Android app demonstrates how to onboard a Bluetooth Low Energy Device and stream data from the device to ARTIK Cloud in real time using WebSockets.

TODO: modify below

Introduction
-------------

The blog post [ArtikCloud and BLE Meet WebSockets
](https://blog.samsungsami.io/mobile/development/2015/04/09/sami-and-ble-meet-websockets.html) at http://blog.samsungsami.io/ describes what the app does and how it is implemented.

Demo
-------------

The sample app has the same functionality as [SAMInBLE](https://github.com/samsungArtikCloudio/sample-android-ArtikCloudnBLE). Check out Demo section there.

Prerequisites
-------------

 * ArtikCloud Android SDK https://github.com/samsungArtikCloudio/ArtikCloud-android
 * [TooTallNate Java WebSockets](https://github.com/TooTallNate/Java-WebSocket)
 * Android SDK v21
 * Android Build Tools v21.1.1
 * Android Studio 1.0.1

Setup and Installation
----------------------

1. Create an Application in devportal.samsungArtikCloud.io:
  * Set "Redirect URL" to `android-app://redirect`.
  * Choose "Client credentials, auth code, implicit" for OAuth 2.0 flow.
  * Under "PERMISSIONS", check "Read" for "Profile". 
  * Click the "Add Device Type" button. Choose "ArtikCloud Example Heart Rate Tracker" as the device type. Check "Read" and "Write" permissions for this device type.
  * [Make a note of the **client ID**.](http://developer.samsungArtikCloud.io/ArtikCloud/ArtikCloud-documentation/developer-user-portals.html#how-to-find-your-application-id) This is an unique application ID.
2. Download and build the [ArtikCloud Android SDK libraries.](https://developer.samsungArtikCloud.io/ArtikCloud/native-SDKs/android-SDK.html) The library JAR files are generated under the `target` and `target/lib` directories of the SDK Maven project. Copy all library JAR files to `Application/libs` of ArtikCloudnBLEws.
3. Download and build [Java WebSockets](https://github.com/TooTallNate/Java-WebSocket). Copy `java_websocket.jar` to `Application/libs` of ArtikCloudnBLEws.
4. Import ArtikCloudnBLEws as an existing Android Studio project in Android Studio IDE.
5. Use the client ID to replace `YOUR CLIENT APP ID` in `ArtikCloudSession.java`.

More about ArtikCloud
---------------

If you are not familiar with ArtikCloud, we have extensive documentation at http://developer.samsungArtikCloud.io

The full ArtikCloud API specification with examples can be found at http://developer.samsungArtikCloud.io/ArtikCloud/api-spec.html

We blog about advanced sample applications at http://blog.samsungArtikCloud.io/

To create and manage your services and devices on ArtikCloud, visit developer portal at http://devportal.samsungArtikCloud.io

License and Copyright
---------------------

Licensed under the Apache License. See LICENSE.

Copyright (c) 2016 Samsung Electronics Co., Ltd.
