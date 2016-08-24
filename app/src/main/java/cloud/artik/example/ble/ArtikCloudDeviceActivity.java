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

import io.samsungsami.model.Device;
import io.samsungsami.model.DeviceArray;
import io.samsungsami.model.DevicesEnvelope;
import io.samsungsami.model.User;
import io.samsungsami.model.UserEnvelope;

public class ArtikCloudDeviceActivity extends ListActivity {
    private static final String TAG = "ArtikCloudDeviceActivity";

    private TextView mWelcome;
    private TextView mInstruction;
    private Button mNewDeviceButton;
    private SAMIDeviceListAdapter mDeviceListAdapter;
    private ArtikCloudDeviceManager mDeviceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samidevices);
        getActionBar().setTitle(R.string.sami_devices_title);

        mNewDeviceButton = (Button)findViewById(R.id.btn);
        mWelcome = (TextView)findViewById(R.id.welcome);
        mInstruction = (TextView)findViewById(R.id.instruction_text);

        mNewDeviceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ":create sami device button is clicked.");
                    createSamiDevice();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        // Initializes list view adapter.
        mDeviceListAdapter = new SAMIDeviceListAdapter();
        setListAdapter(mDeviceListAdapter);

        mDeviceManager = new ArtikCloudDeviceManager();

        ArtikCloudSession.getInstance().setupSamiRestApis();
        new GetUserInfoInBackground().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.samidevices, menu);
        menu.findItem(R.id.menu_logout_sami).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_logout_sami:
                new LogoutSAMIInBackground().execute();
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ArtikCloudDeviceWrapper device = mDeviceListAdapter.getDevice(position);
        if (device == null) return;
        ArtikCloudSession.getInstance().setDeviceId(device.id);
        startBLEScanActivity();
    }

    private void createSamiDevice() {
        new CreateDeviceInBackground().execute();
    }

    private void onDeviceCreationSucceed(Device newDevice) {
        mDeviceManager.updateDevices(newDevice); // single device
        ArtikCloudSession.getInstance().setDeviceId(newDevice.getId());
        Toast.makeText (this, "SAMI device " +newDevice.getId() +"creation succeeded!", Toast.LENGTH_SHORT).show();
        startBLEScanActivity();
    }

    private void refreshDeviceList() {
        ArrayList<ArtikCloudDeviceWrapper> appDevices = mDeviceManager.getDevicesByType(ArtikCloudSession.DEVICE_TYPE_ID_HEART_RATE_TRACKER);
        if (appDevices.size() == 0) {
            mNewDeviceButton.setVisibility(View.VISIBLE);
            mInstruction.setText("You do not have a compatible device on SAMI. Please create one");
            mDeviceListAdapter.clear();
        } else {
            mInstruction.setText("Please select a device on the list to use");
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

    private void onGetUserInfo(User user) {
        if (user == null) {
            return;
        }
        mWelcome.setText("Welcome " + user.getFullName());
        ArtikCloudSession.getInstance().setUserId(user.getId());
        new GetDeviceListInBackground().execute();
    }

    class GetUserInfoInBackground extends AsyncTask<Void, Void, UserEnvelope> {
        final static String TAG = "GetUserInfoInBackground";
        @Override
        protected UserEnvelope doInBackground(Void... params) {
            UserEnvelope retVal = null;
            try {
                retVal= ArtikCloudSession.getInstance().getUsersApi().self();
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }

            return retVal;
        }

        @Override
        protected void onPostExecute(UserEnvelope result) {
            Log.v(TAG, "::setupSamiApi self name = " + result.getData().getFullName());
            onGetUserInfo(result.getData());
        }
    }

    class CreateDeviceInBackground extends AsyncTask<Void, Void, Device> {
        final static String TAG = "CreateDeviceInBackground";
        @Override
        protected Device doInBackground(Void... params) {
            Device retVal = null;
            try {
                Device device = new Device();
                device.setDtid(ArtikCloudSession.DEVICE_TYPE_ID_HEART_RATE_TRACKER);
                device.setUid(ArtikCloudSession.getInstance().getUserId());
                device.setName("ble test device"); //Note this is a limitation --the name is always this one.
                retVal = ArtikCloudSession.getInstance().getDevicesApi().addDevice(device).getData();
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }

            return retVal;
        }

        @Override
        protected void onPostExecute(Device result) {
            Log.v(TAG, "::created device with Id: " + result.getId());
            onDeviceCreationSucceed(result);
        }
    }

    class GetDeviceListInBackground extends AsyncTask<Void, Void, DeviceArray> {
        final static String TAG = "GetDeviceListInBackground";
        @Override
        protected DeviceArray doInBackground(Void... params) {
            DeviceArray deviceArray = null;
            try {
                DevicesEnvelope devicesEnvelope = ArtikCloudSession.getInstance().getUsersApi().getUserDevices(0, 100, false, ArtikCloudSession.getInstance().getUserId());
                deviceArray = devicesEnvelope.getData();

            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }

            return deviceArray;
        }

        @Override
        protected void onPostExecute(DeviceArray devices) {
            mDeviceManager.updateDevices(devices);
            refreshDeviceList();
        }
    }

    class LogoutSAMIInBackground extends AsyncTask<Void, Void, String> {
        final static String TAG = "LogoutSAMIInBackground";
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

    // Adapter for holding SAMI devices, which device type is compatible with this app.
    private class SAMIDeviceListAdapter extends BaseAdapter {
        private ArrayList<ArtikCloudDeviceWrapper> mSAMIDevices;
        private LayoutInflater mInflator;

        public SAMIDeviceListAdapter() {
            super();
            mSAMIDevices = new ArrayList<ArtikCloudDeviceWrapper>();
            mInflator = ArtikCloudDeviceActivity.this.getLayoutInflater();
        }

        public void addDevice(ArtikCloudDeviceWrapper device) {
            if(!mSAMIDevices.contains(device)) {
                mSAMIDevices.add(device);
            }
        }

        public ArtikCloudDeviceWrapper getDevice(int position) {
            return mSAMIDevices.get(position);
        }

        public void clear() {
            mSAMIDevices.clear();
        }

        @Override
        public int getCount() {
            return mSAMIDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mSAMIDevices.get(i);
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
                view = mInflator.inflate(R.layout.listitem_samidevice, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceId = (TextView) view.findViewById(R.id.device_id);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ArtikCloudDeviceWrapper device = mSAMIDevices.get(i);
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
}