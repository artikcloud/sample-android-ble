/*
 * Copyright (C) 2016 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloud.artik.example.ble;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import cloud.artik.api.DevicesApi;
import cloud.artik.api.MessagesApi;
import cloud.artik.api.UsersApi;
import cloud.artik.client.ApiClient;
import cloud.artik.model.Acknowledgement;
import cloud.artik.model.ActionOut;
import cloud.artik.model.MessageIn;
import cloud.artik.model.MessageOut;
import cloud.artik.model.RegisterMessage;
import cloud.artik.model.WebSocketError;
import cloud.artik.websocket.ArtikCloudWebSocketCallback;
import cloud.artik.websocket.DeviceChannelWebSocket;


public class ArtikCloudSession {
    private static final String TAG = ArtikCloudSession.class.getSimpleName();

    public static final String ARTIK_CLOUD_AUTH_BASE_URL = "https://accounts.artik.cloud";
    public static final String CLIENT_ID = "<YOUR CLIENT ID>";
    public static final String REDIRECT_URL = "android-app://redirect";

    // For websocket message
    private static final String HEART_RATE = "heart_rate";


    // ARTIK Cloud device type id used by this app
    // device name: "SAMI Example Heart Rate Tracker"
    // As a hacker, get the device type id using the following ways
    //   -- login to https://api-console.artik.cloud/
    //   -- Click "Get Device Types" api
    //   -- Fill in device name as above
    //   -- Click "Try it"
    //   -- The device type id is "id" field in the response body
    //
    public static final String DEVICE_TYPE_ID_HEART_RATE_TRACKER = "dtaeaf898b4db9418baab77563b7ea2254";

    private static ArtikCloudSession instance;

    private UsersApi mUsersApi = null;
    private DevicesApi mDevicesApi = null;
    private MessagesApi mMessagesApi = null;

    private String mAccessToken = null;
    private String mUserId = null;
    private String mDeviceId = null;

    private DeviceChannelWebSocket mWebsocket;

    private Handler mHandler;

    public static ArtikCloudSession getInstance() {
        if (instance == null) {
            instance = new ArtikCloudSession();
        }
        return instance;
    }

    private ArtikCloudSession() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "Constructor is not called in UI thread ");
        }
        mHandler = new Handler();
    }

    public String getAuthorizationRequestUri() {
        //https://accounts.artik.cloud/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=http://localhost:81/acdemo/index.php
        return ArtikCloudSession.ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + ArtikCloudSession.CLIENT_ID + "&redirect_uri=" + ArtikCloudSession.REDIRECT_URL;
    }

    public String getLogoutRequestUri() {
        //https://accounts.artik.cloud/logout?redirect_uri=http://localhost:81/acdemo/index.php
        return ArtikCloudSession.ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + ArtikCloudSession.CLIENT_ID + "&redirect_uri=" + ArtikCloudSession.REDIRECT_URL;
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set a invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public void setupArtikCloudRestApis() {
        // Invoke the appropriate API
        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken(mAccessToken);
        apiClient.setDebugging(true);

        mUsersApi = new UsersApi(apiClient);
        mDevicesApi = new DevicesApi(apiClient);
        mMessagesApi = new MessagesApi(apiClient);
    }

    public UsersApi getUsersApi() {
        return mUsersApi;
    }

    public DevicesApi getDevicesApi() {
        return mDevicesApi;
    }

    public MessagesApi getMessagesApi() {
        return mMessagesApi;
    }

    public void setUserId(String uid) {
        if (uid == null || uid.length() <= 0) {
            Log.w(TAG, "setUserId() get null uid");
        }
        mUserId = uid;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setDeviceId(String did) {
        if (did == null || did.length() <= 0) {
            Log.w(TAG, "setDeviceId() get null did");
        }
        mDeviceId = did;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public void reset() {
        mUsersApi = null;
        mDevicesApi = null;
        mMessagesApi = null;

        mAccessToken = null;
        mUserId = null;
        mDeviceId = null;

        mWebsocket = null;
    }

    /**
     *
     *
     */
    public void onNewHeartRate(final int heartRate, final long ts) {
        MessageIn message = getWSMessage(heartRate, ts);
        Log.d(TAG, "sendViaWebsocket: send(" + message + ")");
        new SendOnDeviceChannelInBackground().execute(message);
    }

    /**
     *
     *
     */
    public void setupWebsocket() {
        mHandler.post(new Runnable() {
            @Override
            public void run() { connectDeviceChannelWebSocket();}
        });
    }

    /**
     * Setup websocket bidirectional pipeline and register to ARTIK Cloud
     */
    private void connectDeviceChannelWebSocket() {
        try {
            mWebsocket = new DeviceChannelWebSocket(true, new ArtikCloudWebSocketCallback() {
                @Override
                public void onOpen(int i, String s) {
                    final RegisterMessage registerMessage = getWSRegisterMessage();
                    Log.d(TAG, "DeviceChannelWebSocket: onOpen calling websocket.send(" + registerMessage.toString() + ")");

                    try {
                        mWebsocket.registerChannel(registerMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "DeviceChannelWebSocket: onMessage(" + messageOut.toString() + ")");
                }

                @Override
                public void onAction(ActionOut actionOut) {
                    Log.d(TAG, "DeviceChannelWebSocket: onAction(" + actionOut.toString() + ")");
                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                    Log.d(TAG, "DeviceChannelWebSocket: onAck(" + acknowledgement.toString());
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "DeviceChannelWebSocket: onClose() code = " + code + "; reason = " + reason + "; remote = " + remote);
                }

                @Override
                public void onError(WebSocketError error) {
                    Log.d(TAG, "DeviceChannelWebSocket: onError() errorMsg = " + error.getMessage());
                }

                @Override
                public void onPing(long timestamp) {
                    Log.d(TAG, "DeviceChannelWebSocket::onPing: " + timestamp);
                }
            });
        } catch (URISyntaxException|IOException e) {
            e.printStackTrace();
        }

        try {
            mWebsocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectDeviceChannelWebSocket() {
        if (mWebsocket == null) {
            return;
        }
        try {
            mWebsocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SendOnDeviceChannelInBackground extends AsyncTask<MessageIn, Void, Void> {
        final static String TAG = "SendOnDeviceChannel";
        @Override
        protected Void doInBackground(MessageIn... messages) {
            try {
                mWebsocket.sendMessage(messages[0]);
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Do nothing!
        }
    }


    /**
     * Returns JSON payload of the registration message for Bi-directional websocket
     * @return
     */
    private RegisterMessage getWSRegisterMessage() {
        RegisterMessage registerMessage = new RegisterMessage();
        registerMessage.setAuthorization("bearer " + mAccessToken);
        registerMessage.setCid("myRegisterMessage");
        registerMessage.setSdid(mDeviceId);
        return registerMessage;
    }

    /**
     * Returns a full JSON payload to send a message for HeartRateTracker device type
     * @param heartRate
     * @return
     */
    private MessageIn getWSMessage(int heartRate, long ts) {
        MessageIn message = new MessageIn();
        message.setCid("myMessage");
        message.setSdid(mDeviceId);
        message.setTs(ts);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(HEART_RATE, heartRate);
        message.setData(data);
        return message;
    }

}
