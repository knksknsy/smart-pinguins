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

extern "C"
{
#include "nrf_delay.h"
#include "nrf_gpio.h"
#include "nrf.h"
#include "nrf_drv_gpiote.h"
#include "app_error.h"
}

#define PIN_IN 4
#define PIN_OUT 31

AlarmModule::AlarmModule() : Module(ModuleId::ALARM_MODULE, "alarm")
{
	//Start module configuration loading
	configurationPointer = &configuration;
	configurationLength = sizeof(AlarmModuleConfiguration);
	alarmJobHandle = NULL;

	//CONFIG
	lastClusterSize = GS->node.clusterSize;
	nearestTrafficJamNodeId = 0;
	nearestBlackIceNodeId = 0;
	nearestRescueLaneNodeId = 0;
	nearestTrafficJamOppositeLaneNodeId = 0;
	nearestBlackIceOppositeLaneNodeId = 0;
	nearestRescueLaneOppositeLaneNodeId = 0;
	prevDeviceID = 0;

	checkTrafficJamTimer = false;

	GpioInit();

	//Start Broadcasting the informations
	UpdateGpioState();
	RequestAlarmUpdatePacket();
	BroadcastPenguinAdvertisingPacket();
	logt("NODE", "Started MIRO");

	ResetToDefaultConfiguration();
}

void AlarmModule::ButtonHandler(u8 buttonId, u32 holdTimeDs)
{
	//Send alarm update message
	logt("ALARMMOD", "Button pressed %u. Pressed time: %u", buttonId, holdTimeDs);

	BlinkGreenLed();
	UpdateGpioState();

	// Broadcast a rescue lane alarm
	BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::RESCUE_LANE, SERVICE_ACTION_TYPE::SAVE);
}

void AlarmModule::BlinkGreenLed()
{
	GS->ledGreen.On();
	nrf_delay_ms(1000);
	GS->ledGreen.Off();
}

void AlarmModule::ConfigurationLoadedHandler()
{
	//Does basic testing on the loaded configuration
#if IS_INACTIVE(GW_SAVE_SPACE)

#endif
	logt("ALARMMOD", "AlarmModule Config Loaded");
}

/*
 *	RequestAlarmUpdatePacket
 *
 *	sends a broadcast message, requesting an update from other nodes
 *
 */
void AlarmModule::RequestAlarmUpdatePacket()
{
	SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
							0,
							AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE,
							0,
							NULL,
							0,
							false);
}

void AlarmModule::UpdateGpioState()
{
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

void AlarmModule::BroadcastAlarmUpdatePacket(u8 incidentNodeId, SERVICE_INCIDENT_TYPE incidentType, SERVICE_ACTION_TYPE incidentAction, NodeId targetNodeId)
{
	AlarmModuleUpdateMessage data;
	data.meshDeviceId = incidentNodeId;
	data.meshIncidentType = incidentType;
	data.meshActionType = incidentAction;

	SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
							targetNodeId,
							(u8)AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE,
							0,
							(u8 *)&data,
							SIZEOF_ALARM_MODULE_UPDATE_MESSAGE,
							false);
}

/*
 *	BroadcastPenguinAdvertisingPacket
 *
 *	sends a broadcast message with the current node informations
 *
 */
void AlarmModule::BroadcastPenguinAdvertisingPacket()
{
	logt("ALARM_SYSTEM", "Starting Broadcasting Penguin Packet");

	currentAdvChannel = Utility::GetRandomInteger() % 3;

	//build alarm advertisement packet
	AdvJob job = {
		AdvJobTypes::SCHEDULED,			   //JobType
		5,								   //Slots
		0,								   //Delay
		MSEC_TO_UNITS(200, UNIT_0_625_MS), //AdvInterval
		0,								   //AdvChannel
		0,								   //CurrentSlots
		0,								   //CurrentDelay
		GapAdvType::ADV_IND,			   //Advertising Mode
		{0},							   //AdvData
		0,								   //AdvDataLength
		{0},							   //ScanData
		0								   //ScanDataLength
	};

	//Select either the new advertising job or the already existing
	AdvJob *currentJob;
	if (alarmJobHandle == NULL)
	{
		currentJob = &job;
	}
	else
	{
		currentJob = alarmJobHandle;
	}
	u8 *bufferPointer = currentJob->advData;

	advStructureFlags *flags = (advStructureFlags *)bufferPointer;
	flags->len = SIZEOF_ADV_STRUCTURE_FLAGS - 1;
	flags->type = BLE_GAP_AD_TYPE_FLAGS;
	flags->flags = BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE | BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED;

	advStructureUUID16 *serviceUuidList = (advStructureUUID16 *)(bufferPointer + SIZEOF_ADV_STRUCTURE_FLAGS);
	serviceUuidList->len = SIZEOF_ADV_STRUCTURE_UUID16 - 1;
	serviceUuidList->type = BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE;
	serviceUuidList->uuid = SERVICE_DATA_SERVICE_UUID16;

	AdvPacketPenguinData *alarmData = (AdvPacketPenguinData *)(bufferPointer + SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16);
	alarmData->len = SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA - 1;
	alarmData->type = SERVICE_TYPE_ALARM_UPDATE;

	alarmData->uuid = SERVICE_DATA_SERVICE_UUID16;
	alarmData->messageType = SERVICE_DATA_MESSAGE_TYPE_ALARM;
	alarmData->clusterSize = GS->node.clusterSize;
	alarmData->networkId = GS->node.configuration.networkId;

	// Incident data, only send if there actually is an incident (but if there is, need to send all to keep structure)
	if(
			nearestRescueLaneNodeId != 0 || nearestRescueLaneOppositeLaneNodeId != 0 ||
			nearestTrafficJamNodeId != 0 || nearestTrafficJamOppositeLaneNodeId != 0 ||
			nearestBlackIceNodeId != 0 || nearestBlackIceOppositeLaneNodeId != 0
		)
	{
		alarmData->nearestRescueLaneNodeId = nearestRescueLaneNodeId;
		alarmData->nearestTrafficJamNodeId = nearestTrafficJamNodeId;
		alarmData->nearestBlackIceNodeId = nearestBlackIceNodeId;
		alarmData->nearestRescueLaneOppositeLaneNodeId = nearestRescueLaneOppositeLaneNodeId;
		alarmData->nearestTrafficJamOppositeLaneNodeId = nearestTrafficJamOppositeLaneNodeId;
		alarmData->nearestBlackIceOppositeLaneNodeId = nearestBlackIceOppositeLaneNodeId;
	}


	alarmData->advertisingChannel = currentAdvChannel + 1;

	//logt("ALARM_SYSTEM", "unsecureCount: %u", meshDeviceIdArray.size());

	alarmData->nodeId = GS->node.configuration.nodeId;
	alarmData->txPower = Boardconfig->calibratedTX;

	//logt("ALARM_SYSTEM", "txPower: %u", Boardconfig->calibratedTX);

	u32 length = SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16 + SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA;
	job.advDataLength = length;

	//Either update the job or create it if not done
	if (alarmJobHandle == NULL)
	{
		alarmJobHandle = GS->advertisingController.AddJob(job);
		//logt("ALARM_SYSTEM", "NewAdvJob");
	}
	else
	{
		GS->advertisingController.RefreshJob(alarmJobHandle);
		//logt("ALARM_SYSTEM", "Updated the job");
	}
	char cbuffer[100];

	logt("ALARMMOD", "Broadcasting asset data %s, len %u", cbuffer, length);
}

void AlarmModule::ResetToDefaultConfiguration()
{
	//Set default configuration values
	configuration.moduleId = moduleId;
	configuration.moduleActive = true;
	configuration.moduleVersion = 1;
	SET_FEATURESET_CONFIGURATION(&configuration, this);
}

void AlarmModule::MeshMessageReceivedHandler(BaseConnection *connection, BaseConnectionSendData *sendData, connPacketHeader *packetHeader)
{
	//Must call superclass for handling
	Module::MeshMessageReceivedHandler(connection, sendData, packetHeader);

	//Check if this request is meant for modules in general
	if (packetHeader->messageType == MessageType::MODULE_TRIGGER_ACTION)
	{
		logt("ALARMMOD", "Received Alarm Update Request");
		connPacketModule *packet = (connPacketModule *)packetHeader;

		//Check if our module is meant and we should trigger an action
		if (packet->moduleId == moduleId)
		{
			if (packet->actionType == AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE)
			{
				AlarmModuleUpdateMessage *data = (AlarmModuleUpdateMessage *)packet->data;
				logt("ALARMMOD", "Received Alarm Update GET Request");
				// For each incident, check if there is a saved one and if there is, broadcast it out
				if (nearestTrafficJamNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestTrafficJamNodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
				}
				if (nearestBlackIceNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestBlackIceNodeId, SERVICE_INCIDENT_TYPE::BLACK_ICE, SERVICE_ACTION_TYPE::SAVE);
				}
				if (nearestRescueLaneNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestRescueLaneNodeId, SERVICE_INCIDENT_TYPE::RESCUE_LANE, SERVICE_ACTION_TYPE::SAVE);
				}
				if (nearestTrafficJamOppositeLaneNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestTrafficJamOppositeLaneNodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
				}
				if (nearestBlackIceOppositeLaneNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestBlackIceOppositeLaneNodeId, SERVICE_INCIDENT_TYPE::BLACK_ICE, SERVICE_ACTION_TYPE::SAVE);
				}
				if (nearestRescueLaneOppositeLaneNodeId != 0)
				{
					BroadcastAlarmUpdatePacket(nearestRescueLaneOppositeLaneNodeId, SERVICE_INCIDENT_TYPE::RESCUE_LANE, SERVICE_ACTION_TYPE::SAVE);
				}
			}
			if (packet->actionType == AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE)
			{
				AlarmModuleUpdateMessage *data = (AlarmModuleUpdateMessage *)packet->data;
				logt("ALARMMOD", "Received Alarm Update SET Request");

				// If incident got updated, broadcast to mesh and to all other devices
				if (UpdateSavedIncident(data->meshDeviceId, data->meshIncidentType, data->meshActionType))
				{
					BroadcastAlarmUpdatePacket(data->meshDeviceId, (SERVICE_INCIDENT_TYPE)data->meshIncidentType, (SERVICE_ACTION_TYPE)data->meshActionType);
					BroadcastPenguinAdvertisingPacket();
				}
			}
			if (packet->actionType == TrafficJamTriggerActionMessages::TRIGGER_CHECK_LEFT_NODE)
			{
				// Check if traffic jam alarm is active
				if (nearestTrafficJamNodeId == GS->node.configuration.nodeId)
				{
					// Deactivate traffic jam of left node, since it's right node will activate traffic jam alarm
					nearestTrafficJamNodeId = 0;
				}

				// Send response to sender node (predecessor of left node)
				SendModuleActionMessage(MessageType::MODULE_ACTION_RESPONSE,
										packetHeader->sender,
										TrafficJamActionResponseMessages::RESPONSE_FROM_LEFT_NODE,
										0,
										0,
										0,
										false);
			}
			if (packet->actionType == TrafficJamTriggerActionMessages::TRIGGER_CHECK_LEFT_NODE_AT_BACK)
			{
				if (nearestTrafficJamNodeId == GS->node.configuration.nodeId)
				{
					nearestTrafficJamNodeId = 0;
				}

				// Check if left node (back) has traffic jam alert
				SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
										GS->node.configuration.nodeId - 4, // targetNode = right node
										(u8)TrafficJamTriggerActionMessages::TRIGGER_CHECK_RIGHT_NODE_AT_BACK,
										0,
										0,
										0,
										false);
				// Check if right node (back) has traffic jam alert
			}
			if (packet->actionType == TrafficJamTriggerActionMessages::TRIGGER_CHECK_RIGHT_NODE_AT_BACK)
			{
				u8 data[1];
				// Right node has already traffic jam alarm activated => set active alarm => set response
				if (nearestTrafficJamNodeId == GS->node.configuration.nodeId)
				{
					// SAVE alarm 2500m before traffic jam
					nearestTrafficJamNodeId = GS->node.configuration.nodeId;
					BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
					data[0] = 1;
				}
				// Right node has no traffic jam alarm => send response to warning node => set response
				else if (nearestTrafficJamNodeId == 0)
				{
					data[0] = 0;	
				}
				
				// Send response to traffic jam warning node
				SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
										GS->node.configuration.nodeId + 2, // targetNode = right node
										(u8)TrafficJamTriggerActionMessages::TRIGGER_TRAFFIC_JAM_WARNING_NODE,
										0,
										data,
										1,
										false);
			}
			if (packet->actionType == TrafficJamTriggerActionMessages::TRIGGER_TRAFFIC_JAM_WARNING_NODE)
			{
				// Activate alarm since right node has no alarm set
				if (packet->data[0] == 0)
				{
					// SAVE alarm 2500m before traffic jam
					nearestTrafficJamNodeId = GS->node.configuration.nodeId;
					BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::SAVE);
				}
			}
		}
	}

	if (packetHeader->messageType == MessageType::MODULE_ACTION_RESPONSE)
	{
		logt("ALARMMOD", "Received Alarm Action Response");
		connPacketModule *packet = (connPacketModule *)packetHeader;

		if (packet->moduleId == moduleId)
		{
			// @TODO: Handle response timeout
			if (packet->actionType == TrafficJamActionResponseMessages::RESPONSE_FROM_LEFT_NODE)
			{
				// DELETE alarm when traffic jam is reached
				nearestTrafficJamNodeId = GS->node.configuration.nodeId;
				BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::DELETE);

				// Activate alarm at (nodeID - 50)
				SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
										GS->node.configuration.nodeId - ALARM_MODULE_TRAFFIC_JAM_WARNING_RANGE + 2, // targetNode = left node
										(u8)TrafficJamTriggerActionMessages::TRIGGER_CHECK_LEFT_NODE_AT_BACK,
										0,
										0,
										0,
										false);
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
bool AlarmModule::UpdateSavedIncident(u8 incidentNodeId, u8 incidentType, u8 actionType)
{
	bool changed = false;
	SERVICE_INCIDENT_TYPE incType = (SERVICE_INCIDENT_TYPE)incidentType;
	SERVICE_ACTION_TYPE actType = (SERVICE_ACTION_TYPE)actionType;

	// create a generic pointer to the incidentId
	u8 *savedIncidentNodeId = 0;
	if (incType == TRAFFIC_JAM)
	{
		// check if incident id is on same road side
		if ((incidentNodeId - GS->node.configuration.nodeId) % 2 == 0)
		{
			savedIncidentNodeId = &nearestTrafficJamNodeId;
		}
		else
		{
			savedIncidentNodeId = &nearestTrafficJamOppositeLaneNodeId;
		}
	}
	else if (incType == BLACK_ICE)
	{
		if ((incidentNodeId - GS->node.configuration.nodeId) % 2 == 0)
		{
			savedIncidentNodeId = &nearestBlackIceNodeId;
		}
		else
		{
			savedIncidentNodeId = &nearestBlackIceOppositeLaneNodeId;
		}
	}
	else if (incType == RESCUE_LANE)
	{
		if ((incidentNodeId - GS->node.configuration.nodeId) % 2 == 0)
		{
			savedIncidentNodeId = &nearestRescueLaneNodeId;
		}
		else
		{
			savedIncidentNodeId = &nearestRescueLaneOppositeLaneNodeId;
		}
	}

	if (*savedIncidentNodeId == incidentNodeId && actType == DELETE)
	{
		*savedIncidentNodeId = 0;
		changed = true;
		// lane with uneven numbers -> driving direction is 1 -> 3 -> 5 | lane with even numbers, driving direction is 6 -> 4 -> 2 ...
	}
	else if (*savedIncidentNodeId != incidentNodeId && actType == SAVE)
	{
		// incident happened on lane with uneven numbers and is traffic jam or black ice, or happened on even side and is rescue lane
		// results in the same logic, because of the driving directions (one up and one down)
		// and the fact that rescue lane is relevant behind us, while the other are relevant ahead of us
		if ((incidentNodeId % 2 != 0 && (incType == TRAFFIC_JAM || incType == BLACK_ICE)) || (incidentNodeId % 2 == 0 && incType == RESCUE_LANE))
		{
			// ... only new incident with an id smaller than the current saved incident id, but not smaller than our id - 1
			// ( +1 so beacon on same position on other lane is included) are relevant
			// if savedIncidentNodeId is 0, there is no saved incident yet, which means we only have to check if it is ahead of us
			if ((incidentNodeId < *savedIncidentNodeId || *savedIncidentNodeId == 0) && incidentNodeId >= GS->node.configuration.nodeId - 1)
			{
				*savedIncidentNodeId = incidentNodeId;
				changed = true;
			}
		}
		else
		{
			// ... only new incident with an id bigger than the current saved incident id, but not bigger than our id + 1
			// ( +1 so beacon on same position on other lane is included) are relevant
			//  *savedIncidentNodeId == 0 check is not needed here because > comparison always overwrites the 0 case (no saved incident yet)
			if (incidentNodeId > *savedIncidentNodeId && incidentNodeId <= GS->node.configuration.nodeId + 1)
			{
				*savedIncidentNodeId = incidentNodeId;
				changed = true;
			}
		}
	}

	return changed;
}

void AlarmModule::TimerEventHandler (u16 passedTimeDs, u32 appTimerDs)
{
	if (!configuration.moduleActive)
		return;

	if (SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, ALARM_MODULE_BROADCAST_TRIGGER_TIME))
	{
		BroadcastPenguinAdvertisingPacket();
	}
	// Traffic jam timer: toggles bool to check for still standing vehicles
	if (SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, ALARM_MODULE_TRAFFIC_JAM_DETECTION_TIME_DS))
	{
		checkTrafficJamTimer = true;
	}
}

void AlarmModule::GpioInit()
{
	nrf_gpio_pin_dir_set(PIN_OUT, NRF_GPIO_PIN_DIR_OUTPUT);
	nrf_gpio_cfg_output(PIN_OUT);
	nrf_gpio_pin_set(PIN_OUT);
	nrf_gpio_cfg_input(PIN_IN, NRF_GPIO_PIN_NOPULL);
}

void AlarmModule::GapAdvertisementReportEventHandler(const GapAdvertisementReportEvent &advertisementReportEvent)
{
	if (!configuration.moduleActive)
		return;

	const advPacketCarServiceAndDataHeader *packetHeader = (const advPacketCarServiceAndDataHeader *)advertisementReportEvent.getData();

	if (packetHeader->mway_service_uuid == 0xFE12 && packetHeader->mway_service_uuid2 == 0xFE12)
	{
		const AdvPacketCarData *packetData = (const AdvPacketCarData *)&packetHeader->data;

		// Logging hex values of packetHeader
		const advPacketCarServiceAndDataHeader header = *packetHeader;
		unsigned char *rawDataPtr1 = (unsigned char *)&header;
		u16 size1 = sizeof(header);
		logt("ALARMMOD", "raw data (advPacketCarServiceAndDataHeader):\n");
		while (size1--)
		{
			logt("ALARMMOD", "0x%02X", *rawDataPtr1++);
		}
		// Logging hex values of packetData
		const AdvPacketCarData data = *packetData;
		unsigned char *rawDataPtr2 = (unsigned char *)&data;
		u16 size2 = sizeof(data);
		logt("ALARMMOD", "raw data (AdvPacketCarData):\n");
		while (size2--)
		{
			logt("ALARMMOD", "0x%02X", *rawDataPtr2++);
		}
		// Logging values of packetHeader
		logt("ALARMMOD", "advPacketCarServiceAndDataHeader:\n");
		logt("ALARMMOD", "flags: 0x%02X,\nmway_service_uuid: 0x%02X,\nflags2: 0x%02X\nmway_service_uuid2: 0x%02X\n",
			 packetHeader->flags,
			 packetHeader->mway_service_uuid,
			 packetHeader->flags2,
			 packetHeader->mway_service_uuid2);
		// Logging values of packetData
		logt("ALARMMOD", "advPacketAssetServiceData:\nlen: 0x%02X,\ntype: 0x%02X,\nmessageType: 0x%02X,\ndeviceID: 0x%02X,\ndeviceType: 0x%02X,\ndirection: 0x%02X,\nisEmergency: 0x%02X,\nisSlippery: 0x%02X,\nisJam: 0x%02X",
			 packetData->len,
			 packetData->type,
			 packetData->messageType,
			 packetData->deviceID,
			 packetData->deviceType,
			 packetData->direction,
			 packetData->isEmergency,
			 packetData->isSlippery,
			 packetData->isJam);

		// Check for same directions of beacon and vehicle
		if (packetData->direction == GS->node.configuration.direction)
		{
			// Save deviceID of nearby vehicle
			if (!checkTrafficJamTimer)
			{
				prevDeviceID = packetData->deviceID;
			}
			// Check for same deviceID after 5s timer (see TimerEventHandler)
			// Vehicle is not moving => initiate traffic jam alarm
			else if (prevDeviceID == packetData->deviceID)
			{
				// Check if left node has traffic jam alarm activated
				SendModuleActionMessage(MessageType::MODULE_TRIGGER_ACTION,
										GS->node.configuration.nodeId + 2, // targetNode = left node
										(u8)TrafficJamTriggerActionMessages::TRIGGER_CHECK_LEFT_NODE,
										0,
										0,
										0,
										false);
				checkTrafficJamTimer = false;
			}
			// Vehicle is moving => deactivate traffic jam alarm
			else
			{
				nearestTrafficJamNodeId = 0;
				BroadcastAlarmUpdatePacket(GS->node.configuration.nodeId, SERVICE_INCIDENT_TYPE::TRAFFIC_JAM, SERVICE_ACTION_TYPE::DELETE);
				checkTrafficJamTimer = false;
			}
		}
	}
}
