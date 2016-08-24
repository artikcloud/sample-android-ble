/*
 * Copyright (C) 2015 Samsung Electronics Co., Ltd.
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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

import io.samsungsami.api.DevicesApi;
import io.samsungsami.api.MessagesApi;
import io.samsungsami.api.UsersApi;
import io.samsungsami.websocket.Acknowledgement;
import io.samsungsami.websocket.ActionOut;
import io.samsungsami.websocket.DeviceChannelWebSocket;
import io.samsungsami.websocket.Error;
import io.samsungsami.websocket.MessageIn;
import io.samsungsami.websocket.MessageOut;
import io.samsungsami.websocket.RegisterMessage;
import io.samsungsami.websocket.SamiWebSocketCallback;

public class ArtikCloudSession {
    private static final String TAG = ArtikCloudSession.class.getSimpleName();

    public static final String SAMI_AUTH_BASE_URL = "https://accounts.samsungsami.io";
    public static final String CLIENT_ID = "b120963174a84eff897770624f829ee5";//YWU
    public static final String REDIRECT_URL = "android-app://redirect";
    public static final String SAMI_REST_URL = "https://api.samsungsami.io/v1.1";

    private static final String AUTHORIZATION = "Authorization";

    // For websocket message
    private static final String HEART_RATE = "heart_rate";


    // SAMI device type id used by this app
    // device name: "SAMI Example Heart Rate Tracker"
    // As a hacker, get the device type id using the following ways
    //   -- login to https://api-console.samsungsami.io/sami
    //   -- Click "Get Device Types" api
    //   -- Fill in device name as above
    //   -- Click "Try it"
    //   -- The device type id is "id" field in the response body
    // You should be able to get this device type id programmatically.
    // Consult https://blog.samsungsami.io/mobile/development/2015/02/09/developing-with-sami-part-2.html#get-the-withings-device-info
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
        //https://accounts.samsungsami.io/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=http://localhost:81/samidemo/index.php
        return ArtikCloudSession.SAMI_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + ArtikCloudSession.CLIENT_ID + "&redirect_uri=" + ArtikCloudSession.REDIRECT_URL;
    }

    public String getLogoutRequestUri() {
        //https://accounts.samsungsami.io/logout?redirect_uri=http://localhost:81/samidemo/index.php
        return ArtikCloudSession.SAMI_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
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

    public void setupSamiRestApis() {
        // Invoke the appropriate API
        mUsersApi = new UsersApi();
        mUsersApi.setBasePath(SAMI_REST_URL);
        mUsersApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);

        mDevicesApi = new DevicesApi();
        mDevicesApi.setBasePath(SAMI_REST_URL);
        mDevicesApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);

        mMessagesApi = new MessagesApi();
        mMessagesApi.setBasePath(SAMI_REST_URL);
        mMessagesApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);
    }

    public void logout() {
        reset();
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
        mHandler.post(new Runnable() {
            @Override
            public void run() { sendViaWebsocket(heartRate, ts);}
        });
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
     * Setup websocket bidirectional pipeline and register to SAMI
     */
    private void connectDeviceChannelWebSocket() {
        try {
            mWebsocket = new DeviceChannelWebSocket(true, new SamiWebSocketCallback() {
                @Override
                public void onOpen(short i, String s) {
                    final RegisterMessage registerMessage = getWSRegisterMessage();
                    Log.d(TAG, "WebSocket: onOpen calling websocket.send(" + registerMessage.toString() + ")");

                    try {
                        mWebsocket.registerChannel(registerMessage);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "WebSocket: onMessage(" + messageOut.toString() + ")");
                }

                @Override
                public void onAction(ActionOut actionOut) {
                    Log.d(TAG, "WebSocket: onAction(" + actionOut.toString() + ")");
                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                    Log.d(TAG, "WebSocket: onAck(" + acknowledgement.toString());
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket: onClose() code = " + code + "; reason = " + reason + "; remote = " + remote);
                }

                @Override
                public void onError(Error error) {
                    Log.d(TAG, "WebSocket: onError() errorMsg = " + error.getMessage());
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mWebsocket.connect();
    }

    public void disconnectDeviceChannelWebSocket() {
        if (mWebsocket != null ) {
            mWebsocket.close();
        }
    }

    /**
     * Connects to /websocket
     *
     */
    private void sendViaWebsocket(final int heartRate, final long ts) {
        MessageIn message = getWSMessage(heartRate, ts);
        try {
            mWebsocket.sendMessage(message);
            Log.d(TAG, "sendViaWebsocket: send(" + message +")");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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
        message.setTs(BigDecimal.valueOf(ts));
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(HEART_RATE, heartRate);
        message.setData(data);
        return message;
    }

}
