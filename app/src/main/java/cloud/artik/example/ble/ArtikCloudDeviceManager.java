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


import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import io.samsungsami.model.DeviceArray;

public class ArtikCloudDeviceManager {
	public ArrayList<ArtikCloudDeviceWrapper> userDevices = new ArrayList<ArtikCloudDeviceWrapper>();
	private static final String TAG = ArtikCloudDeviceManager.class.getName();
	
	public ArtikCloudDeviceManager(){
		clearCache();
	}
	
	/**
	 * Clear the cache of devices in memory
	 */
	public void clearCache(){
		userDevices.clear();
	}
	
	/**
	 * Loads a new set of devices
	 * @param result result from the SAMI users devices API call
	 */
	public void updateDevices(JSONObject result){
		JSONArray jsonData = null;
    	try {
    		jsonData = result.getJSONObject("data").getJSONArray("devices");
		}
    	catch (Exception e) {
			Log.d(TAG, "Error parsing result to get devices.");
		}
		clearCache();
		
		if (jsonData != null){	
			for(int i=0; i<jsonData.length(); i++){ 
				JSONObject device;
				try {
					device = (JSONObject) jsonData.getJSONObject(i);
					userDevices.add(new ArtikCloudDeviceWrapper(
							device.getString("dtid"), 
							device.getString("id"), 
							device.getString("name")));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Loads the list of a user's devices
	 * @param deviceArray
	 * @param storeIt
	 */
	public void updateDevices(DeviceArray deviceArray){
		clearCache();
		if(deviceArray != null){
			for (io.samsungsami.model.Device device : deviceArray.getDevices()){
				userDevices.add(new ArtikCloudDeviceWrapper(device.getDtid(), device.getId(), device.getName()));
			}
		}
	}

    /**
     * Loads a single user's devices
     * @param device
     * @param storeIt
     */
    public void updateDevices(io.samsungsami.model.Device device){
        clearCache();
        userDevices.add(new ArtikCloudDeviceWrapper(device.getDtid(), device.getId(), device.getName()));
    }
	/**
	 * Returns a JSON string from a credentials object
	 * @return
	 */
	public static String toJson(io.samsungsami.model.Device device){
		ObjectMapper mapper = new ObjectMapper();
		String json = null;
		try {
			json = mapper.writeValueAsString(device);
		} catch (JsonGenerationException ex) {
			ex.printStackTrace();
		} catch (JsonMappingException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return json;
	}
	
	/**
	 * Returns if the current stack has devices
	 * @return
	 */
	public boolean hasDevices(){
		return userDevices.size() > 0;
	}
	
	/**
	 * Returns a list of devices from the current devices stack
	 * @param dtid device type to filter
	 * @return
	 */
	public ArrayList<ArtikCloudDeviceWrapper> getDevicesByType(String dtid){
		ArrayList<ArtikCloudDeviceWrapper> result = new ArrayList<ArtikCloudDeviceWrapper>();
		for(ArtikCloudDeviceWrapper device : userDevices){
			if(device.deviceTypeId.equalsIgnoreCase(dtid)){
				result.add(device);
			}
		}
		return result;
	}
	
	/**
	 * Get an array for listpreference entries
	 * @return
	 */
	public CharSequence[] getCharSequenceEntries(){
		CharSequence[] entries = new CharSequence[userDevices.size()+1];
		entries[0] = "";
    	for(int i=0;i<userDevices.size();i++){
    		entries[i+1] = userDevices.get(i).name;
    	}
    	return entries;
	}
	 
	/**
	 * Get an array for listpreference values
	 * @return
	 */
	public CharSequence[] getCharSequenceEntriesValues(){
		CharSequence[] entries = new CharSequence[userDevices.size()+1];
		entries[0] = "";
		for(int i=0;i<userDevices.size();i++){
    		entries[i+1] = userDevices.get(i).id;
    	}
    	return entries;
	}
	
}
