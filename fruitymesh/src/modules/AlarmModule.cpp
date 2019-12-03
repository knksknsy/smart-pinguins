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

#include <AlarmModule.h>

#include <Logger.h>
#include <algorithm>
#include <vector>
#include <Node.h>
#include <Utility.h>
#include <math.h>
#include <GlobalState.h>

#include <stdbool.h>

extern "C" {
#include "nrf_delay.h"
#include "nrf_gpio.h"
#include "nrf.h"
#include "nrf_drv_gpiote.h"
#include "app_error.h"
}

#define PIN_IN 4
#define PIN_OUT 31

AlarmModule::AlarmModule() :
		Module(ModuleId::ALARM_MODULE, "alarm") {
	//Start module configuration loading
	configurationPointer = &configuration;
	configurationLength = sizeof(AlarmModuleConfiguration);
	alarmJobHandle = NULL;

	//CONFIG
	lastClusterSize = GS->node.clusterSize;
	nearestTrafficJamNodeId = 0;
	nearestBlackIceNodeId = 0;
	nearestRescueLaneNodeId = 0;

	GpioInit();

	//Start Broadcasting the informations
	UpdateGpioState();
	RequestAlarmUpdatePacket();
	BroadcastPenguinAdvertisingPacket();
	logt("NODE", "Started MIRO");

	ResetToDefaultConfiguration();

}

void AlarmModule::ButtonHandler(u8 buttonId, u32 holdTimeDs) {
	//Send alarm update message
	logt("CONFIG", "Button pressed %u. Pressed time: %u", buttonId, holdTimeDs);

	BlinkGreenLed();
	UpdateGpioState();

	// Broadcast a rescue lane alarm
	BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::RESCUE_LANE, SERVICE_ACTION_TYPE::SAVE);
}

void AlarmModule::BlinkGreenLed() {
	GS->ledGreen.On();
	nrf_delay_ms(1000);
	GS->ledGreen.Off();
}

void AlarmModule::ConfigurationLoadedHandler() {
	//Does basic testing on the loaded configuration
#if IS_INACTIVE(GW_SAVE_SPACE)

#endif
	logt("CONFIG", "AlarmModule Config Loaded");

}

/*
 *	RequestAlarmUpdatePacket
 *
 *	sends a broadcast message, requesting an update from other nodes
 *
 */
void AlarmModule::RequestAlarmUpdatePacket() {
	SendModuleActionMessage(
			MessageType::MODULE_TRIGGER_ACTION,
			0,
			AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE,
			0,
			NULL,
			0,
			false);
}

void AlarmModule::UpdateGpioState() {
	nrf_gpio_pin_set(PIN_OUT);
	gpioState = nrf_gpio_pin_read(PIN_IN);
	nrf_gpio_pin_clear(PIN_OUT);
}

/*
 *	BroadcastAlarmUpdatePacket
 *
 *	sends a broadcast alarm message with the specified incident nodeId, type and action
 *
 *	u8 incidentNodeId
 *	SERVICE_INCIDENT_TYPE incidentType
 *	SERVICE_ACTION_TYPE incidentAction

 */

void AlarmModule::BroadcastAlarmUpdatePacket(u8 incidentNodeId, SERVICE_INCIDENT_TYPE incidentType, SERVICE_ACTION_TYPE incidentAction) {
	AlarmModuleUpdateMessage data;
	data.meshDeviceId = incidentNodeId;
	data.meshIncidentType = incidentType;
	data.meshActionType = incidentAction;

	SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
			0,
			(u8) AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE,
			0,
			(u8*) &data,
			SIZEOF_ALARM_MODULE_UPDATE_MESSAGE,
			false);
}

/*
 *	BroadcastPenguinAdvertisingPacket
 *
 *	sends a broadcast message with the current node informations
 *
 */
void AlarmModule::BroadcastPenguinAdvertisingPacket() {
	logt("ALARM_SYSTEM", "Starting Broadcasting Penguin Packet");

	currentAdvChannel = Utility::GetRandomInteger() % 3;

	//build alarm advertisement packet
	AdvJob job = { AdvJobTypes::SCHEDULED, //JobType
			5, //Slots
			0, //Delay
			MSEC_TO_UNITS(200, UNIT_0_625_MS), //AdvInterval
			0, //AdvChannel
			0, //CurrentSlots
			0, //CurrentDelay
			GapAdvType::ADV_IND, //Advertising Mode
			{ 0 }, //AdvData
			0, //AdvDataLength
			{ 0 }, //ScanData
			0 //ScanDataLength
			};

	//Select either the new advertising job or the already existing
	AdvJob* currentJob;
	if (alarmJobHandle == NULL) {
		currentJob = &job;
	} else {
		currentJob = alarmJobHandle;
	}
	u8* bufferPointer = currentJob->advData;

	advStructureFlags* flags = (advStructureFlags*) bufferPointer;
	flags->len = SIZEOF_ADV_STRUCTURE_FLAGS - 1;
	flags->type = BLE_GAP_AD_TYPE_FLAGS;
	flags->flags = BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE
			| BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED;

	advStructureUUID16* serviceUuidList = (advStructureUUID16*) (bufferPointer
			+ SIZEOF_ADV_STRUCTURE_FLAGS);
	serviceUuidList->len = SIZEOF_ADV_STRUCTURE_UUID16 - 1;
	serviceUuidList->type = BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE;
	serviceUuidList->uuid = SERVICE_DATA_SERVICE_UUID16;

	AdvPacketPenguinData* alarmData = (AdvPacketPenguinData*) (bufferPointer
			+ SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16);
	alarmData->len = SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA - 1;
	alarmData->type = SERVICE_TYPE_ALARM_UPDATE;

	alarmData->uuid = SERVICE_DATA_SERVICE_UUID16;
	alarmData->messageType = SERVICE_DATA_MESSAGE_TYPE_ALARM;
	alarmData->clusterSize = GS->node.clusterSize;
	alarmData->networkId = GS->node.configuration.networkId;

	// Incident data
	alarmData->nearestRescueLaneNodeId = nearestRescueLaneNodeId;
	alarmData->nearestTrafficJamNodeId = nearestTrafficJamNodeId;
	alarmData->nearestBlackIceNodeId = nearestBlackIceNodeId;

	alarmData->advertisingChannel = currentAdvChannel + 1;


	//logt("ALARM_SYSTEM", "unsecureCount: %u", meshDeviceIdArray.size());

	alarmData->nodeId = GS->node.configuration.nodeId;
	alarmData->txPower = Boardconfig->calibratedTX;

	//logt("ALARM_SYSTEM", "txPower: %u", Boardconfig->calibratedTX);

	u32 length = SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16
			+ SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA;
	job.advDataLength = length;

	//Either update the job or create it if not done
	if (alarmJobHandle == NULL) {
		alarmJobHandle = GS->advertisingController.AddJob(job);
		//logt("ALARM_SYSTEM", "NewAdvJob");

	} else {
		GS->advertisingController.RefreshJob(alarmJobHandle);
		//logt("ALARM_SYSTEM", "Updated the job");
	}
	char cbuffer[100];

	logt("CONFIG", "Broadcasting asset data %s, len %u", cbuffer, length);

}

void AlarmModule::ResetToDefaultConfiguration() {
	//Set default configuration values
	configuration.moduleId = moduleId;
	configuration.moduleActive = true;
	configuration.moduleVersion = 1;
	SET_FEATURESET_CONFIGURATION(&configuration, this);

}

void AlarmModule::MeshMessageReceivedHandler(BaseConnection* connection,
		BaseConnectionSendData* sendData, connPacketHeader* packetHeader) {

	//Must call superclass for handling
	Module::MeshMessageReceivedHandler(connection, sendData, packetHeader);
	connPacketModule* packet = (connPacketModule*) packetHeader;

	logt("CONFIG", "Action Type, %d", packet->actionType);
	logt("CONFIG", "Message type, %d", (int)packetHeader->messageType);
	AlarmModuleUpdateMessage* data =
			(AlarmModuleUpdateMessage*) packet->data;

	logt("CONFIG", "INCIDENT NODE ID %u",data->meshDeviceId);
	logt("CONFIG", "MESH DATA TYPE %u", data->meshIncidentType);
	logt("CONFIG", "MESH ACTION TYPE %u", data->meshActionType);

	//Check if this request is meant for modules in general
	if (packetHeader->messageType == MessageType::MODULE_TRIGGER_ACTION) {
		logt("CONFIG", "Received Alarm Update Request");
		connPacketModule* packet = (connPacketModule*) packetHeader;

		//Check if our module is meant and we should trigger an action
		if (packet->moduleId == moduleId) {
			if (packet->actionType
					== AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE) {
				logt("CONFIG", "Received Alarm Update GET Request");

				// TODO: send current states
			}
			if (packet->actionType
					== AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE) {
				logt("CONFIG", "Received Alarm Update SET Request");

				if(CheckForIncidentUpdate(data->meshDeviceId, data->meshIncidentType, data->meshActionType)) {
					BroadcastAlarmUpdatePacket(data->meshDeviceId, (SERVICE_INCIDENT_TYPE)data->meshIncidentType, (SERVICE_ACTION_TYPE)data->meshActionType);
				}
			}
		}
	}
}
;

bool AlarmModule::CheckForIncidentUpdate(u8 incidentNodeId, u8 incidentType, u8 actionType) {
	bool changed = false;
	SERVICE_INCIDENT_TYPE incType = (SERVICE_INCIDENT_TYPE)incidentType;
	SERVICE_ACTION_TYPE actType = (SERVICE_ACTION_TYPE)actionType;
	u8 * savedIncidentId = 0;
	if(incType == RESCUE_LANE) {
		savedIncidentId = &nearestRescueLaneNodeId;
	} else if(incType == TRAFFIC_JAM) {
		savedIncidentId = &nearestTrafficJamNodeId;
	} else if(incType == BLACK_ICE) {
		savedIncidentId = &nearestBlackIceNodeId;
	}

	if(*savedIncidentId == incidentNodeId) {
		if(actType == DELETE) {
			*savedIncidentId = 0;
			changed = true;
		}
	} else {
		if(actType == SAVE) {
			*savedIncidentId = incidentNodeId;
			changed = true;
		}
	}
	return changed;
}

void AlarmModule::TimerEventHandler(u16 passedTimeDs, u32 appTimerDs) {
	if (!configuration.moduleActive)
		return;

	if (SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs,
			ALARM_MODULE_BROADCAST_TRIGGER_TIME)) {
		BroadcastPenguinAdvertisingPacket();
	}
}

void AlarmModule::GpioInit() {
	nrf_gpio_pin_dir_set(PIN_OUT, NRF_GPIO_PIN_DIR_OUTPUT);
	nrf_gpio_cfg_output(PIN_OUT);
	nrf_gpio_pin_set(PIN_OUT);
	nrf_gpio_cfg_input(PIN_IN, NRF_GPIO_PIN_NOPULL);
}

void AlarmModule::GapAdvertisementReportEventHandler(const GapAdvertisementReportEvent& advertisementReportEvent)
{
	if (!configuration.moduleActive) return;

#if IS_INACTIVE(GW_SAVE_SPACE)
	HandleAssetV2Packets(advertisementReportEvent);
#endif
}

#define _______________________ASSET_V2______________________

#if IS_INACTIVE(GW_SAVE_SPACE)
//This function checks whether we received an assetV2 packet
void AlarmModule::HandleAssetV2Packets(const GapAdvertisementReportEvent& advertisementReportEvent)
{
	const advPacketServiceAndDataHeader* packet = (const advPacketServiceAndDataHeader*)advertisementReportEvent.getData();
	const advPacketAssetServiceData* assetPacket = (const advPacketAssetServiceData*)&packet->data;

	//Check if the advertising packet is an asset packet
	if (
			advertisementReportEvent.getDataLength() >= SIZEOF_ADV_STRUCTURE_ASSET_SERVICE_DATA
			&& packet->flags.len == SIZEOF_ADV_STRUCTURE_FLAGS-1
			&& packet->uuid.len == SIZEOF_ADV_STRUCTURE_UUID16-1
			&& packet->data.type == BLE_GAP_AD_TYPE_SERVICE_DATA
			&& packet->data.uuid == SERVICE_DATA_SERVICE_UUID16
			&& packet->data.messageType == SERVICE_DATA_MESSAGE_TYPE_ASSET
	){
		char serial[6];
		Utility::GenerateBeaconSerialForIndex(assetPacket->serialNumberIndex, serial);
		logt("SCANMOD", "RX ASSETV2 ADV: serial %s, pressure %u, speed %u, temp %u, humid %u, cn %u, rssi %d",
				serial,
				assetPacket->pressure,
				assetPacket->speed,
				assetPacket->temperature,
				assetPacket->humidity,
				assetPacket->advertisingChannel,
				advertisementReportEvent.getRssi());


		//Adds the asset packet to our buffer
		addTrackedAsset(assetPacket, advertisementReportEvent.getRssi());

	}
}

/**
 * Finds a free slot in our buffer of asset packets and adds the packet
 */
bool AlarmModule::addTrackedAsset(const advPacketAssetServiceData* packet, i8 rssi){
	if(packet->serialNumberIndex == 0) return false;

	rssi = -rssi; //Make rssi positive

	if(rssi < 10 || rssi > 90) return false; //filter out wrong rssis

	scannedAssetTrackingPacket* slot = nullptr;

	//Look for an old entry of this asset or a free space
	//Because we fill this buffer from the beginning, we can use the first slot that is empty
	for(int i = 0; i<ASSET_PACKET_BUFFER_SIZE; i++){
		if(assetPackets[i].serialNumberIndex == packet->serialNumberIndex || assetPackets[i].serialNumberIndex == 0){
			slot = &assetPackets[i];
			break;
		}
	}

	//If a slot was found, add the packet
	if(slot != nullptr){
		u16 slotNum = ((u32)slot - (u32)assetPackets.getRaw()) / sizeof(scannedAssetTrackingPacket);
		logt("SCANMOD", "Tracked packet %u in slot %d", packet->serialNumberIndex, slotNum);

		//Clean up first, if we overwrite another assetId
		if(slot->serialNumberIndex != packet->serialNumberIndex){
			slot->serialNumberIndex = packet->serialNumberIndex;
			slot->count = 0;
			slot->rssi37 = slot->rssi38 = slot->rssi39 = UINT8_MAX;
		}
		//If the count is at its max, we reset the rssi
		if(slot->count == UINT8_MAX){
			slot->count = 0;
			slot->rssi37 = slot->rssi38 = slot->rssi39 = UINT8_MAX;
		}

		slot->serialNumberIndex = packet->serialNumberIndex;
		slot->count++;
		//Channel 0 means that we have no channel data, add it to all rssi channels
		if(packet->advertisingChannel == 0 && rssi < slot->rssi37){
			slot->rssi37 = (u16) rssi;
			slot->rssi38 = (u16) rssi;
			slot->rssi39 = (u16) rssi;
		}
		if(packet->advertisingChannel == 1 && rssi < slot->rssi37) slot->rssi37 = (u16) rssi;
		if(packet->advertisingChannel == 2 && rssi < slot->rssi38) slot->rssi38 = (u16) rssi;
		if(packet->advertisingChannel == 3 && rssi < slot->rssi39) slot->rssi39 = (u16) rssi;
		slot->direction = packet->direction;
		slot->pressure = packet->pressure;
		slot->speed = packet->speed;

		return true;
	}
	return false;
}
#endif

