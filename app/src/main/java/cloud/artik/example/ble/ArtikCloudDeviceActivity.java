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

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cloud.artik.client.ApiCallback;
import cloud.artik.client.ApiException;
import cloud.artik.model.Device;
import cloud.artik.model.DeviceArray;
import cloud.artik.model.DeviceEnvelope;
import cloud.artik.model.DevicesEnvelope;
import cloud.artik.model.User;
import cloud.artik.model.UserEnvelope;

public class ArtikCloudDeviceActivity extends ListActivity {
    private static final String TAG = "AKCDeviceActivity";

    private TextView mWelcome;
    private TextView mInstruction;
    private Button mNewDeviceButton;
    private ArtikCloudDeviceListAdapter mDeviceListAdapter;
    private ArtikCloudDeviceManager mDeviceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //YWU TODO Remove temp code
        Log.d(TAG, "Enter onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artikcloud_devices);
        getActionBar().setTitle(R.string.artikcloud_devices_title);

        mNewDeviceButton = (Button)findViewById(R.id.btn);
        mWelcome = (TextView)findViewById(R.id.welcome);
        mInstruction = (TextView)findViewById(R.id.instruction_text);

        mNewDeviceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ":new device button is clicked");
                    addDevice();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        // Initializes list view adapter.
        mDeviceListAdapter = new ArtikCloudDeviceListAdapter();
        setListAdapter(mDeviceListAdapter);

        mDeviceManager = new ArtikCloudDeviceManager();

        ArtikCloudSession.getInstance().setupArtikCloudRestApis();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.artikcloud_devices, menu);
        menu.findItem(R.id.menu_logout_cloud).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_logout_cloud:
                new LogoutArtikCloudInBackground().execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        // Disable going back to the previous screen
    }

    @Override
    public void onResume() {
        //YWU TODO Remove temp code
        Log.d(TAG, "Enter onResume");
        super.onResume();
        getUserInfo();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ArtikCloudDeviceWrapper device = mDeviceListAdapter.getDevice(position);
        if (device == null) return;
        ArtikCloudSession.getInstance().setDeviceId(device.id);
        startBLEScanActivity();
    }

    private void handleDeviceCreationSuccessOnUIThread(final Device newDevice) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "ARTIK Cloud device " + newDevice.getId() + "creation succeeded!", Toast.LENGTH_SHORT).show();
                mDeviceManager.updateDevices(newDevice); // single device
                ArtikCloudSession.getInstance().setDeviceId(newDevice.getId());
                startBLEScanActivity();
            }
        });
    }

    private void UpdateDeviceListOnUIThread(final DeviceArray deviceArray) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceManager.updateDevices(deviceArray);
                refreshDeviceList();            }
        });
    }

    private void refreshDeviceList() {
        ArrayList<ArtikCloudDeviceWrapper> appDevices = mDeviceManager.getDevicesByType(ArtikCloudSession.DEVICE_TYPE_ID_HEART_RATE_TRACKER);
        mDeviceListAdapter.clear();
        if (appDevices.size() == 0) {
            mNewDeviceButton.setVisibility(View.VISIBLE);
            mInstruction.setText(R.string.create_artikcloud_device_hint);
        } else {
            mInstruction.setText(R.string.select_device);
            mNewDeviceButton.setVisibility(View.GONE);
            for (ArtikCloudDeviceWrapper device : appDevices) {
                mDeviceListAdapter.addDevice(device);
            }
        }
        mDeviceListAdapter.notifyDataSetChanged();

    }
    private void startBLEScanActivity() {
        Log.d(TAG, "StartBLEScanActivity calling startActivity()");
        ArtikCloudSession.getInstance().setupWebsocket();
        Intent activityIntent = new Intent(this, DeviceScanActivity.class);
        startActivity(activityIntent);
    }

    private void startLoginActivity() {
        ArtikCloudSession.getInstance().reset();
        Intent activityIntent = new Intent(this, LoginActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(activityIntent);
        finish();
    }

    private void handleUserInfoOnUIThread(final User user) {
        if (user == null) {
            return;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWelcome.setText("Welcome " + user.getFullName());
                ArtikCloudSession.getInstance().setUserId(user.getId());
                GetDeviceList();
            }
        });
    }

    private void getUserInfo()
    {
        final String tag = TAG + " getSelfAsync";
        if (ArtikCloudSession.getInstance().getUserId() != null) {
            GetDeviceList();
            return;
        }
        try {
            ArtikCloudSession.getInstance().getUsersApi().getSelfAsync(new ApiCallback<UserEnvelope>() {
                @Override
                public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                    processFailure(tag, exc);
                }

                @Override
                public void onSuccess(cloud.artik.model.UserEnvelope result, int statusCode, Map<String, List<String>> map) {
                    Log.v(tag, " onSuccess() self name = " + result.getData().getFullName());
                    handleUserInfoOnUIThread(result.getData());
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void addDevice() {
        final String tag = TAG + " addDeviceAsync";
        cloud.artik.model.Device device = new cloud.artik.model.Device();
        device.setDtid(ArtikCloudSession.DEVICE_TYPE_ID_HEART_RATE_TRACKER);
        device.setUid(ArtikCloudSession.getInstance().getUserId());
        device.setName("ble test device"); //Note this is a limitation --the name is always this one.
        try {
            ArtikCloudSession.getInstance().getDevicesApi().addDeviceAsync(device, new ApiCallback<DeviceEnvelope>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    Log.e(tag, "onFailure: e = " + e + "; statusCode = " + statusCode);
                    processFailure(tag, e);
                }

                @Override
                public void onSuccess(DeviceEnvelope result, int statusCode, Map<String, List<String>> responseHeaders) {
                    Log.v(tag, " onSuccess " + result.toString());
                    handleDeviceCreationSuccessOnUIThread(result.getData());
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void GetDeviceList() {
        final String tag = TAG + " getUserDevicesAsync";
        try {
            ArtikCloudSession.getInstance().getUsersApi().getUserDevicesAsync(ArtikCloudSession.getInstance().getUserId(),
                    null, null, false, new ApiCallback<DevicesEnvelope>() {
                        @Override
                        public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                            Log.e(tag, "onFailure: e = " + e + "; statusCode = " + statusCode);
                            processFailure(tag, e);
                        }

                        @Override
                        public void onSuccess(DevicesEnvelope result, int statusCode, Map<String, List<String>> responseHeaders) {
                            Log.v(tag, " onSuccess " + result.toString());
                            UpdateDeviceListOnUIThread(result.getData());
                        }

                        @Override
                        public void onUploadProgress(long bytes, long contentLen, boolean done) {
                        }

                        @Override
                        public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                        }

                    });

        }  catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    //YWU TODO what is this? Do we need it?
    class LogoutArtikCloudInBackground extends AsyncTask<Void, Void, String> {
        final static String TAG = "LogoutInBackground";
        @Override
        protected String doInBackground(Void... params) {
            ArtikCloudSession.getInstance().disconnectDeviceChannelWebSocket();
            HttpClient httpclient = new DefaultHttpClient();
            String responseString = null;
            HttpGet request = new HttpGet(ArtikCloudSession.getInstance().getLogoutRequestUri());
            Log.d(TAG, "logoutRequestUri: " + ArtikCloudSession.getInstance().getLogoutRequestUri());

            try {
                HttpResponse response = httpclient.execute(request);
                int code = response.getStatusLine().getStatusCode();
                StringBuilder sbuilder = new StringBuilder("response code:");
                sbuilder.append(code);
                responseString = sbuilder.toString();
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String response) {
            Log.v(TAG, "::onPostExecute(" + response +")");
            startLoginActivity();
        }
    }

    // Adapter for holding ARTIK Cloud devices, which device type is compatible with this app.
    private class ArtikCloudDeviceListAdapter extends BaseAdapter {
        private ArrayList<ArtikCloudDeviceWrapper> mArtikCloudDevices;
        private LayoutInflater mInflator;

        public ArtikCloudDeviceListAdapter() {
            super();
            mArtikCloudDevices = new ArrayList<>();
            mInflator = ArtikCloudDeviceActivity.this.getLayoutInflater();
        }

        public void addDevice(ArtikCloudDeviceWrapper device) {
            if(!mArtikCloudDevices.contains(device)) {
                mArtikCloudDevices.add(device);
            }
        }

        public ArtikCloudDeviceWrapper getDevice(int position) {
            return mArtikCloudDevices.get(position);
        }

        public void clear() {
            mArtikCloudDevices.clear();
        }

        @Override
        public int getCount() {
            return mArtikCloudDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mArtikCloudDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_artikcloud_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceId = (TextView) view.findViewById(R.id.device_id);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ArtikCloudDeviceWrapper device = mArtikCloudDevices.get(i);
            final String deviceName = device.name;
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceId.setText(device.id);

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceId;
    }

    ///// Helpers
    private void processFailure(final String context, ApiException exc) {
        String errorDetail = " onFailure with exception" + exc;
        Log.w(context, errorDetail);
        exc.printStackTrace();
    }
}
