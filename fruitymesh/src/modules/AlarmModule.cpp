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
	logt("ALRAMMOD", "Button pressed %u. Pressed time: %u", buttonId, holdTimeDs);

	BlinkGreenLed();
	UpdateGpioState();

	// Broadcast a rescue lane alarm
	BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
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
	logt("ALRAMMOD", "AlarmModule Config Loaded");

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
	alarmData->nearestRescueLaneNodeId = 151;

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

	logt("ALRAMMOD", "Broadcasting asset data %s, len %u", cbuffer, length);

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
	AlarmModuleUpdateMessage* data =
			(AlarmModuleUpdateMessage*) packet->data;

	//Check if this request is meant for modules in general
	if (packetHeader->messageType == MessageType::MODULE_TRIGGER_ACTION) {
		logt("ALRAMMOD", "Received Alarm Update Request");
		connPacketModule* packet = (connPacketModule*) packetHeader;

		//Check if our module is meant and we should trigger an action
		if (packet->moduleId == moduleId) {
			if (packet->actionType
					== AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE) {
				logt("ALRAMMOD", "Received Alarm Update GET Request");
				// For each incident, check if there is a saved one and if there is, broadcast it out
				if(nearestTrafficJamNodeId != 0) {
					BroadcastAlarmUpdatePacket(nearestTrafficJamNodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
				}
				if(nearestBlackIceNodeId != 0) {
					BroadcastAlarmUpdatePacket(nearestBlackIceNodeId, SERVICE_INCIDENT_TYPE::BLACK_ICE, SERVICE_ACTION_TYPE::SAVE);
				}
				if(nearestRescueLaneNodeId != 0) {
					BroadcastAlarmUpdatePacket(nearestRescueLaneNodeId, SERVICE_INCIDENT_TYPE::RESCUE_LANE, SERVICE_ACTION_TYPE::SAVE);
				}
			}
			if (packet->actionType
					== AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE) {
				logt("ALRAMMOD", "Received Alarm Update SET Request");

				// If incident got updated, broadcast to mesh and to all other devices
				if(UpdateSavedIncident(data->meshDeviceId, data->meshIncidentType, data->meshActionType)) {
					BroadcastAlarmUpdatePacket(data->meshDeviceId, (SERVICE_INCIDENT_TYPE)data->meshIncidentType, (SERVICE_ACTION_TYPE)data->meshActionType);
					BroadcastPenguinAdvertisingPacket();
				}
			}
		}
	}
}
/* UpdateSavedIncident, updates a saved incident, if it is relevant
 *
 * @param u8 incidentNodeId, the id of the node where an incident happened / vanished
 * @param u8 incidentType, the type of incident, one of SERVICE_INCIDENT_TYPE
 * @param u8 actionType, the action type, one of SERVICE_ACTION_TYPE
 *
 * returns bool changed, true if saved incident got updated, false if not
 */
bool AlarmModule::UpdateSavedIncident(u8 incidentNodeId, u8 incidentType, u8 actionType) {
	bool changed = false;
	SERVICE_INCIDENT_TYPE incType = (SERVICE_INCIDENT_TYPE)incidentType;
	SERVICE_ACTION_TYPE actType = (SERVICE_ACTION_TYPE)actionType;

	// Rescue lane differs from other two incident types because the threat is behind us
	if(incType == RESCUE_LANE) {
		if(nearestRescueLaneNodeId == incidentNodeId && actType == DELETE) {
			nearestRescueLaneNodeId = 0;
			changed = true;
		} else if(nearestRescueLaneNodeId != incidentNodeId && actType == SAVE) {
			// for relevance -> check if incident id is on same road side
			if((incidentNodeId - GS->node.configuration.nodeId) % 2 == 0) {
				// road side with uneven numbers
				if(GS->node.configuration.nodeId % 2 != 0) {
					// on uneven side driving direction is 1 -> 3 -> 5 ...
					// only new incident with an id bigger than the current saved incident id, but not bigger than our id (ahead of us) are relevant
					if(incidentNodeId > nearestRescueLaneNodeId && incidentNodeId < GS->node.configuration.nodeId) {
						nearestRescueLaneNodeId = incidentNodeId;
						changed = true;
					}
				// road side with even numbers
				} else {
					// on even side driving direction is 6 -> 4 -> 2 ...
					// only new incident with an id smaller than the current saved incident id, but not smaller than our id (ahead of us) are relevant
					// if nearestRescueLaneNodeId is 0, there is no saved incident yet, which means we only have to check if it is behind us
					if((incidentNodeId < nearestRescueLaneNodeId || nearestRescueLaneNodeId == 0) && incidentNodeId > GS->node.configuration.nodeId) {
						nearestRescueLaneNodeId = incidentNodeId;
						changed = true;
					}
				}
			}
		}
	// The logic of the other two incident types can be put together because the threat is ahead of us
	} else {
		// create a generic pointer to the incidentId
		u8 * savedIncidentNodeId = 0;
		if(incType == TRAFFIC_JAM) {
			savedIncidentNodeId = &nearestTrafficJamNodeId;
		} else if(incType == BLACK_ICE) {
			savedIncidentNodeId = &nearestBlackIceNodeId;
		}

		if(*savedIncidentNodeId == incidentNodeId && actType == DELETE) {
			*savedIncidentNodeId = 0;
			changed = true;
		} else if(*savedIncidentNodeId != incidentNodeId && actType == SAVE) {
			// for relevance -> check if incident id is on same road side
			if((incidentNodeId - GS->node.configuration.nodeId) % 2 == 0) {
				// road side with uneven numbers
				if(GS->node.configuration.nodeId % 2 != 0) {
					// on uneven side driving direction is 1 -> 3 -> 5 ...
					// only new incident with an id smaller than the current saved incident id, but not smaller than our id (behind us) are relevant
					// if savedIncidentNodeId is 0, there is no saved incident yet, which means we only have to check if it is ahead of us
					if((incidentNodeId < *savedIncidentNodeId || *savedIncidentNodeId == 0) && incidentNodeId > GS->node.configuration.nodeId) {
						*savedIncidentNodeId = incidentNodeId;
						changed = true;
					}
				// road side with even numbers
				} else {
					// on even side driving direction is 6 -> 4 -> 2 ...
					// only new incident with an id bigger than the current saved incident id, but not bigger than our id (behind us) are relevant
					if(incidentNodeId > *savedIncidentNodeId && incidentNodeId < GS->node.configuration.nodeId ) {
						*savedIncidentNodeId = incidentNodeId;
						changed = true;
					}
				}
			}
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

	const advPacketServiceAndDataHeader* packet = (const advPacketServiceAndDataHeader*)advertisementReportEvent.getData();
	const advPacketAssetServiceData* assetPacket = (const advPacketAssetServiceData*)&packet->data;

	// FIXME: Filter UUID
	if (packet->data.uuid == 4503 && packet->uuid.uuid == 4630) {

		logt("ALRAMMOD", "advPacketServiceAndDataHeader:\n");
		logt("ALRAMMOD", "advStructureFlags:\nlen: %u,\ntype: %u,\nflags: %u\n",
				packet->flags.len,
				packet->flags.type,
				packet->flags.flags
			);
		logt("ALRAMMOD", "advStructureUUID16:\nlen: %u,\ntype: %u,\nuuid: %u\n",
				packet->uuid.len,
				packet->uuid.type,
				packet->uuid.uuid
			);
		logt("ALRAMMOD", "advStructureServiceDataAndType:\nlen: %u,\ntype: %u,\nuuid: %u,\nmessageType: %u",
				packet->data.len,
				packet->data.type,
				packet->data.uuid,
				packet->data.messageType
			);
		logt("ALRAMMOD", "\n--------------------\n");

		logt("ALRAMMOD", "advPacketAssetServiceData:\nmway_servicedata: %u,\nlen: %u,\ntype: %u,\nmessageType: %u,\nadvChannel: %u,\ndeviceType: %u,\ndirection: %u,\nisEmergency: %u,\nisSlippery: %u",
				assetPacket->mway_servicedata,
				assetPacket->len,
				assetPacket->type,
				assetPacket->messageType,
				assetPacket->advChannel,
				assetPacket->deviceType,
				assetPacket->direction,
				assetPacket->isEmergency,
				assetPacket->isSlippery
			);
		logt("ALRAMMOD", "\n--------------------\n");
	}
}
