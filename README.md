# ARTIK Cloud sample app AKCnBLE

The sample Android app demonstrates how to onboard a Bluetooth Low Energy Device and stream data from the device to ARTIK Cloud in real time using WebSockets.

TODO: modify below

Introduction
-------------

The blog post [SAMI and BLE Meet WebSockets
](https://blog.samsungsami.io/mobile/development/2015/04/09/sami-and-ble-meet-websockets.html) at http://blog.samsungsami.io/ describes what the app does and how it is implemented.

Demo
-------------

The sample app has the same functionality as [SAMInBLE](https://github.com/samsungsamiio/sample-android-SAMInBLE). Check out Demo section there.

Prerequisites
-------------

 * SAMI Android SDK https://github.com/samsungsamiio/sami-android
 * [TooTallNate Java WebSockets](https://github.com/TooTallNate/Java-WebSocket)
 * Android SDK v21
 * Android Build Tools v21.1.1
 * Android Studio 1.0.1

Setup and Installation
----------------------

1. Create an Application in devportal.samsungsami.io:
  * Set "Redirect URL" to `android-app://redirect`.
  * Choose "Client credentials, auth code, implicit" for OAuth 2.0 flow.
  * Under "PERMISSIONS", check "Read" for "Profile". 
  * Click the "Add Device Type" button. Choose "SAMI Example Heart Rate Tracker" as the device type. Check "Read" and "Write" permissions for this device type.
  * [Make a note of the **client ID**.](http://developer.samsungsami.io/sami/sami-documentation/developer-user-portals.html#how-to-find-your-application-id) This is an unique application ID.
2. Download and build the [SAMI Android SDK libraries.](https://developer.samsungsami.io/sami/native-SDKs/android-SDK.html) The library JAR files are generated under the `target` and `target/lib` directories of the SDK Maven project. Copy all library JAR files to `Application/libs` of SAMInBLEws.
3. Download and build [Java WebSockets](https://github.com/TooTallNate/Java-WebSocket). Copy `java_websocket.jar` to `Application/libs` of SAMInBLEws.
4. Import SAMInBLEws as an existing Android Studio project in Android Studio IDE.
5. Use the client ID to replace `YOUR CLIENT APP ID` in `SAMISession.java`.

More about SAMI
---------------

If you are not familiar with SAMI, we have extensive documentation at http://developer.samsungsami.io

The full SAMI API specification with examples can be found at http://developer.samsungsami.io/sami/api-spec.html

We blog about advanced sample applications at http://blog.samsungsami.io/

To create and manage your services and devices on SAMI, visit developer portal at http://devportal.samsungsami.io

License and Copyright
---------------------

Licensed under the Apache License. See LICENSE.

Copyright (c) 2015 Samsung Electronics Co., Ltd.
