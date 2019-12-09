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
	u8 meshDeviceId; // node id
	u8 meshIncidentType; // type of incident, e.g traffic jam, one of SERVICE_INCIDENT_TYPE
	u8 meshActionType; // incident type action, e.g SAVE or DELETE, one of SERVICE_ACTION_TYPE
}AlarmModuleUpdateMessage;

#define SERVICE_DATA_MESSAGE_TYPE_ALARM 25
#define SERVICE_TYPE_ALARM_UPDATE 33
#define ALARM_MODULE_BROADCAST_TRIGGER_TIME 300
#define ASSET_PACKET_BUFFER_SIZE 30

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
 
    //3 byte additional beacon information
    u8 nodeId;
    i8 txPower;
    u8 boardType; //Nur für Debugging
 
    //3 byte cluster information
    u8 currentClusterSize;
    u8 clusterSize;
    u8 networkId; //Nur für Debugging
 
    //3 Byte Penguin Information
	u8 nearestTrafficJamNodeId;
	u8 nearestBlackIceNodeId;
	u8 nearestRescueLaneNodeId;
}AdvPacketPenguinData;

typedef struct {
	u16 mway_servicedata;
    //6 byte header
    u8 len;  
    u8 type; 
    u16 uuid;
    u16 messageType;
 
    //3 byte car information (Können/sollten auch nur als Bits gesetzt werden)
    u8 deviceType; // Car, bicycle, pedestrian
	u8 direction; // 1 = North, 2 = East, 12 = NorthEast etc-
	u8 isEmergency;
    u8 isSlippery;
}AdvPacketCarData;

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

	enum BoardType {
		DEV_BOARD = 1,
		RUUVI_TAG = 3
	};

	//Storage for advertising packets
	typedef struct
	{
		u32 serialNumberIndex;
		u8 rssi37;
		u8 rssi38;
		u8 rssi39;
		u8 count;
		u8 speed;
		u8 direction;
		u16 pressure;
	} scannedAssetTrackingPacket;

	SimpleArray<scannedAssetTrackingPacket, ASSET_PACKET_BUFFER_SIZE> assetPackets;

#define SIZEOF_MA_MODULE_DISCONNECT_MESSAGE 7
	typedef struct
	{
		fh_ble_gap_addr_t targetAddress;

	} MeshAccessModuleDisconnectMessage;

	enum SERVICE_INCIDENT_TYPE {
		RESCUE_LANE = 0,
		TRAFFIC_JAM = 1,
		BLACK_ICE = 2,
	};
	enum SERVICE_ACTION_TYPE {
		DELETE = 0,
		SAVE = 1,
	};

	u8 nearestTrafficJamNodeId;
	u8 nearestBlackIceNodeId;
	u8 nearestRescueLaneNodeId;

	void HandleAssetV2Packets(const GapAdvertisementReportEvent& advertisementReportEvent);
	bool addTrackedAsset(const advPacketAssetServiceData* packet, i8 rssi);

	AlarmModuleConfiguration configuration;
	AdvJob* alarmJobHandle;
	u8 currentAdvChannel;
	u8 index;

	u8 lastClusterSize;
	u8 gpioState;
#pragma pack(pop)

public:
	AlarmModule();
	void MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader);

	void ButtonHandler(u8 buttonId, u32 holdTimeDs);

	void ConfigurationLoadedHandler();

	void ResetToDefaultConfiguration();

	void BroadcastPenguinAdvertisingPacket();

	void BroadcastAlarmUpdatePacket(u8 incidentNodeId, SERVICE_INCIDENT_TYPE incidentType, SERVICE_ACTION_TYPE incidentAction);

	void RequestAlarmUpdatePacket();

	bool CheckForIncidentUpdate(u8 incidentNodeId, u8 incidentType, u8 actionType);

	void TimerEventHandler(u16 passedTimeDs, u32 appTimerDs);

	void ReceivedMeshAccessDisconnectMessage(connPacketModule* packet, u16 packetLength);

	void GpioInit();

	void BlinkRedLed();

	void BlinkGreenLed();

	void UpdateGpioState();

	virtual void GapAdvertisementReportEventHandler(const GapAdvertisementReportEvent& advertisementReportEvent) override;
};


