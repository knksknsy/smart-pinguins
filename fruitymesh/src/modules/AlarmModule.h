/**

 Copyright (c) 2014-2017 "M-Way Solutions GmbH"
 FruityMesh - Bluetooth Low Energy mesh protocol [http://mwaysolutions.com/]

 This file is part of FruityMesh

 FruityMesh is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

#pragma once

#include <Module.h>


#include <AdvertisingController.h>
#include <BoardConfig.h>
#include "vector"

#define SIZEOF_ALARM_MODULE_UPDATE_MESSAGE 5

typedef struct {
	u8 meshDeviceId;
	u8 meshDataType;
	u8 meshActionType;
	u8 meshDataOne;
	i8 meshDataTwo;
}AlarmModuleUpdateMessage;

#define SERVICE_DATA_MESSAGE_TYPE_ALARM 25
#define SERVICE_TYPE_ALARM_UPDATE 33
#define SERVICE_DATA_TYPE_WINDOW_VERSION_ONE 78
#define ALARM_MODULE_BROADCAST_TRIGGER_TIME 300
#define ALARM_MODULE_SENSOR_SCAN_TRIGGER_TIME 100


#define TEMPERATURE_NO_VALUE  -128
#define HUMIDITY_NO_VALUE  0xFF

//Service Data (max. 24 byte)
#define SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA 22 //ToDo
typedef struct {
	//6 byte header
	u8 len;
	u8 type;
	u16 uuid;
	u16 messageType;
	//1 byte capabilities
	u8 advertisingChannel :2;// 0 = not available, 1=37, 2=38, 3=39

	//5 byte additional beacon information
	u8 nodeId;
	i8 txPower;
	u8 boardType;
	i8 temperature;
	u8 humidity;

	//1 Byte active spots
	u8 active;

	//3 byte cluster information
	u8 clusterSize;
	u8 clusterId;
	u8 networkId;

	//6 Byte Spot Information
	u8 meshDataType;
	u8 spotCount;
	u8 index;
	u8 meshDeviceId;
	i8 meshDataOne;
	u8 meshDataTwo;

}advPacketAlarmData;

class AlarmModule: public Module {
private:
#pragma pack(push, 1)
	//Module configuration that is saved persistently (size must be multiple of 4)

	struct AlarmModuleConfiguration: ModuleConfiguration
	{
		//Insert more persistent config values here
	};

	enum AlarmModuleTriggerActionMessages {
		MA_CONNECT = 0,
		MA_DISCONNECT = 1,
		SET_ALARM_SYSTEM_UPDATE = 2,
		GET_ALARM_SYSTEM_UPDATE = 3
	};

	enum AlarmModuleActionResponseMessages {
		ALARM_SYSTEM_UPDATE = 1
	};

	enum AlarmModuleActiveStates {
		ACTIVE = 1,
		INACTIVE = 0
	};

	enum BoardType {
		DEV_BOARD = 1,
		RUUVI_TAG = 3
	};

#define SIZEOF_MA_MODULE_DISCONNECT_MESSAGE 7
	typedef struct
	{
		fh_ble_gap_addr_t targetAddress;

	}MeshAccessModuleDisconnectMessage;


	//Für NC REED invertieren
	enum AlarmModuleStates {
		DELETE = 1, SAVE = 0
	};

	void InitialiseBME280();
	void UpdateBarometerData();

	AlarmModuleConfiguration configuration;

	std::vector<u8> meshDeviceIdArray;
	std::vector<u8> meshDataTypeArray;
	std::vector<u8> meshDataOneArray;
	std::vector<u8> meshDataTwoArray;

	AdvJob* alarmJobHandle;
	u8 currentAdvChannel;
	u8 isWindowOpen;
	u8 index;

	//barometer
	u32 lastBarometerReadTimeDs;
	i32 lastTemperatureReading;
	i8 stateChangedTemperatureReading;
	u32 lastHumidityReading;
	u8 lastClusterSize;
	u8 gpioState;
	bool isActivated;
#pragma pack(pop)

public:
	AlarmModule();
	void MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader);

	void ButtonHandler(u8 buttonId, u32 holdTimeDs);

	void ConfigurationLoadedHandler();

	void ResetToDefaultConfiguration();

	void BroadcastAlarmAdvertisingPacket();

	void BroadcastAlarmUpdatePacket();

	void RequestAlarmUpdatePacket();

	void TimerEventHandler(u16 passedTimeDs, u32 appTimerDs);

	void ReceivedMeshAccessDisconnectMessage(connPacketModule* packet, u16 packetLength);

	void GpioInit();

	void BlinkRedLed();

	void BlinkGreenLed();

	void UpdateGpioState();
};


