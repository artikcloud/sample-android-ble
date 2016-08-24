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

public class ArtikCloudDeviceWrapper {
	public static final String PROPERTY_DEVICE_TYPE_ID = "dtid";
	public static final String PROPERTY_SOURCE_DEVICE_ID = "sdid";
	public static final String PROPERTY_DATE = "ts";
	public static final int DEVICE_NAME_MIN_LENGTH = 5;
	public String deviceTypeId;
	public String id;
	public String name;
	
	public ArtikCloudDeviceWrapper(String deviceTypeId, String id, String name){
		this.deviceTypeId = deviceTypeId;
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return name+" ("+id+")";
	}
	
	
	
}
