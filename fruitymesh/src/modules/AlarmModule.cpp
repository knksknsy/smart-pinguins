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

#ifdef ACTIVATE_ALARM_MODULE

#include <Logger.h>
#include <algorithm>
#include <vector>
#include <Node.h>
#include <Utility.h>
#include <Math.h>

#include <stdbool.h>

extern "C" {
#include "bme280.h"
#include "nrf_delay.h"
#include "nrf_gpio.h"
#include "nrf.h"
#include "nrf_drv_gpiote.h"
#include "app_error.h"
}

#define PIN_IN 4
#define PIN_OUT 31
#define ALARM_MODULE_BAROMETER_SLEEP_DS 150

AlarmModule::AlarmModule(u8 moduleId, Node* node, ConnectionManager* cm, const char* name)
: Module(moduleId, node, cm, name)
{
	moduleVersion = 1;
	//Start module configuration loading
	configurationPointer = &configuration;
	configurationLength = sizeof(AlarmModuleConfiguration);
	alarmJobHandle = NULL;

	LoadModuleConfiguration();

	lastTemperatureReading = 0;
	lastHumidityReading = 0;
	lastBarometerReadTimeDs = 0;
	isWindowOpen = false;
	//logt("ALARM_SYSTEM", "nodeId %u", GS->config->defaultNodeId);
	lastClusterSize = GS->node->clusterSize;

	//CONFIG

	isActivated = false;
	GpioInit();

	//Start Broadcasting the informations
	UpdateGpioState();
	RequestAlarmUpdatePacket();
	BroadcastAlarmAdvertisingPacket();

}

void AlarmModule::ButtonHandler(u8 buttonId, u32 holdTimeDs) {
	//Send alarm update message
	logt("ALARM_SYSTEM", "Button pressed %u. Pressed time: %u", buttonId,holdTimeDs);
	isActivated = !isActivated;
	isActivated ? BlinkGreenLed():BlinkRedLed();

	UpdateGpioState();

	BroadcastAlarmUpdatePacket();
	RequestAlarmUpdatePacket();
}

void AlarmModule::BlinkGreenLed() {
	GS->ledGreen->On();
	nrf_delay_ms(1000);
	GS->ledGreen->Off();
}

void AlarmModule::BlinkRedLed() {
	GS->ledRed->On();
	nrf_delay_ms(1000);
	GS->ledRed->Off();
}

void AlarmModule::ConfigurationLoadedHandler()
{
	//Does basic testing on the loaded configuration
	Module::ConfigurationLoadedHandler();
	currentAdvChannel = 0;

	if(configuration.moduleActive) {

		//Enable barometer if available
		if(Boardconfig->spiM0SSBmePin != -1 && configuration.enableBarometer) {
			InitialiseBME280();
		}
	}

}
void AlarmModule::RequestAlarmUpdatePacket() {
	SendModuleActionMessage(
			MESSAGE_TYPE_MODULE_TRIGGER_ACTION,
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

void AlarmModule::BroadcastAlarmUpdatePacket() {
	AlarmModuleUpdateMessage data;
	data.meshDeviceId = GS->config->defaultNodeId;

	data.meshActionType= isActivated ? gpioState : 1;
	data.meshDataType = SERVICE_DATA_TYPE_WINDOW_VERSION_ONE;

	if(configuration.enableBarometer && Boardconfig->spiM0SSBmePin != -1) {
		UpdateBarometerData();

		data.meshDataOne = (i8)lastTemperatureReading;
		data.meshDataTwo = (u8)lastHumidityReading;
	} else {
		data.meshDataOne = 0xFF;
		data.meshDataTwo = 0xFF;
	}

	SendModuleActionMessage(
			MESSAGE_TYPE_MODULE_ACTION_RESPONSE,
			0,
			AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE,
			0,
			(u8*)&data,
			SIZEOF_ALARM_MODULE_UPDATE_MESSAGE,
			false
	);

}

//void in_pin_handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action)
//{
//	AlarmModule::isAlarmTriggered = true;
//}

void AlarmModule::BroadcastAlarmAdvertisingPacket() {
	//logt("ALARM_SYSTEM", "Starting Broadcasting");

	currentAdvChannel = Utility::GetRandomInteger() % 3;

	//build alarm advertisement packet
	AdvJob job = {
		AdvJobTypes::ADV_JOB_TYPE_SCHEDULED, //JobType
		5,//Slots
		0,//Delay
		MSEC_TO_UNITS(200, UNIT_0_625_MS),//AdvInterval
		0,//AdvChannel
		0,//CurrentSlots
		0,//CurrentDelay
		BLE_GAP_ADV_TYPE_ADV_IND,//Advertising Mode
		{	0}, //AdvData
		0,//AdvDataLength
		{	0}, //ScanData
		0//ScanDataLength
	};

	//Select either the new advertising job or the already existing
	AdvJob* currentJob;
	if(alarmJobHandle == NULL) {
		currentJob = &job;
	} else {
		currentJob = alarmJobHandle;
	}
	u8* bufferPointer = currentJob->advData;

	advStructureFlags* flags = (advStructureFlags*)bufferPointer;
	flags->len = SIZEOF_ADV_STRUCTURE_FLAGS-1;
	flags->type = BLE_GAP_AD_TYPE_FLAGS;
	flags->flags = BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE | BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED;

	advStructureUUID16* serviceUuidList = (advStructureUUID16*)(bufferPointer+SIZEOF_ADV_STRUCTURE_FLAGS);
	serviceUuidList->len = SIZEOF_ADV_STRUCTURE_UUID16 - 1;
	serviceUuidList->type = BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE;
	serviceUuidList->uuid = SERVICE_DATA_SERVICE_UUID16;

	advPacketAlarmData* alarmData = (advPacketAlarmData*)(bufferPointer+SIZEOF_ADV_STRUCTURE_FLAGS+SIZEOF_ADV_STRUCTURE_UUID16);
	alarmData->len = SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA - 1;
	alarmData->type = SERVICE_TYPE_ALARM_UPDATE;

	alarmData->uuid = SERVICE_DATA_SERVICE_UUID16;
	alarmData->messageType = SERVICE_DATA_MESSAGE_TYPE_ALARM;
	alarmData->clusterSize = GS->node->clusterSize;
	alarmData->clusterId = GS->node->clusterId;
	alarmData->networkId = GS->config->defaultNetworkId;
	alarmData->active = isActivated ? AlarmModuleActiveStates::ACTIVE : AlarmModuleActiveStates::INACTIVE;

	if(configuration.enableBarometer && Boardconfig->spiM0SSBmePin != -1) {
		UpdateBarometerData();
		alarmData->temperature = (i8)lastTemperatureReading;
		alarmData->humidity = (u8)lastHumidityReading;
		alarmData->boardType = BoardType::RUUVI_TAG;

	} else {
		//logt("ALARM_SYSTEM", "No BME on board");
		alarmData->temperature = TEMPERATURE_NO_VALUE;
		alarmData->humidity = HUMIDITY_NO_VALUE;
		alarmData->boardType = BoardType::DEV_BOARD;
	}

	alarmData->advertisingChannel = currentAdvChannel + 1;

	if(index >= (u8) meshDeviceIdArray.size()) {
		index = 0;
	}
	alarmData->index = index;
	alarmData->spotCount = meshDeviceIdArray.size();
	if(meshDeviceIdArray.size() == 0 ) {
		alarmData->meshDataType = 0;
		alarmData->meshDeviceId = 0;
		alarmData->meshDataOne = 0;
		alarmData->meshDataTwo = 0;
	} else {
		logt("ALARM_SYSTEM", "Index %u, Device %u, Temp %u, Hum %u", index,
				meshDeviceIdArray.at(index),
				meshDataOneArray.at(index),
				meshDataTwoArray.at(index));
		alarmData->meshDataType = meshDataTypeArray.at(index);
		alarmData->meshDeviceId = meshDeviceIdArray.at(index);
		alarmData->meshDataOne = meshDataOneArray.at(index);
		alarmData->meshDataTwo = meshDataTwoArray.at(index);
	}
	index++;

	//logt("ALARM_SYSTEM", "unsecureCount: %u", meshDeviceIdArray.size());

	alarmData->nodeId = GS->config->defaultNodeId;
	alarmData->txPower = Boardconfig->calibratedTX;

	//logt("ALARM_SYSTEM", "txPower: %u", Boardconfig->calibratedTX);

	u32 length = SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16 + SIZEOF_ADV_STRUCTURE_ALARM_SERVICE_DATA;
	job.advDataLength = length;

	//Either update the job or create it if not done
	if(alarmJobHandle == NULL) {
		alarmJobHandle = AdvertisingController::getInstance()->AddJob(&job);
		//logt("ALARM_SYSTEM", "NewAdvJob");

	} else {
		AdvertisingController::getInstance()->RefreshJob(alarmJobHandle);
		//logt("ALARM_SYSTEM", "Updated the job");
	}
	char cbuffer[100];
	Logger::getInstance()->convertBufferToHexString(alarmJobHandle->advData, length, cbuffer, 100);
	logt("ALARM_SYSTEM", "Broadcasting asset data %s, len %u", cbuffer, length);

}

void AlarmModule::ResetToDefaultConfiguration()
{
	//Set default configuration values
	configuration.moduleId = moduleId;
	configuration.moduleActive = true;
	configuration.moduleVersion = 1;
	configuration.enableBarometer = true;

}

//
// 2 windows open. close upper upper one. reopen - > wrong hum and temp
//
//

void AlarmModule::MeshMessageReceivedHandler(BaseConnection* connection,
		BaseConnectionSendData* sendData, connPacketHeader* packetHeader) {

	//Must call superclass for handling
	Module::MeshMessageReceivedHandler(connection, sendData, packetHeader);

	//Check if this request is meant for modules in general
	if (packetHeader->messageType == MESSAGE_TYPE_MODULE_TRIGGER_ACTION) {

		connPacketModule* packet = (connPacketModule*) packetHeader;

		//Check if our module is meant and we should trigger an action
		if (packet->moduleId == moduleId) {

			if(packet->actionType == AlarmModuleTriggerActionMessages::GET_ALARM_SYSTEM_UPDATE) {
				logt("ALARM_SYSTEM", "Received Alarm Update Request");
				BroadcastAlarmUpdatePacket();
			}
		}
	} else if (packetHeader->messageType == MESSAGE_TYPE_MODULE_ACTION_RESPONSE) {
		connPacketModule* packet = (connPacketModule*) packetHeader;
		if(packet->actionType == AlarmModuleTriggerActionMessages::SET_ALARM_SYSTEM_UPDATE) {

			AlarmModuleUpdateMessage* data = (AlarmModuleUpdateMessage*)packet->data;
			logt("ALARM_SYSTEM", "ALARM_UPDATE_RECEIVED type %u",data->meshActionType);

			//Not part of the list, add it to it
			if(!(std::find(meshDeviceIdArray.begin(), meshDeviceIdArray.end(), data->meshDeviceId) != meshDeviceIdArray.end())) {
				if (data->meshActionType == AlarmModuleStates::SAVE) {
					meshDeviceIdArray.push_back(data->meshDeviceId);
					meshDataTypeArray.push_back(data->meshDataType);
					meshDataOneArray.push_back(data->meshDataOne);
					meshDataTwoArray.push_back(data->meshDataTwo);
					logt("ALARM_SYSTEM", "NOT FOUND: positionId %u temp %u hum %u added  ",
							data->meshDeviceId,
							data->meshDataOne,
							data->meshDataTwo);

					logt("ALARM_SYSTEM", "NOT FOUND: idSize %u tempSize %u humSize %u added  ",
							meshDeviceIdArray.size(),
							meshDataOneArray.size(),
							meshDataTwoArray.size());
				}

			}
			//Part of the list, find the index
			else {
				int index = 0;
				for (std::size_t i = 0; i != meshDeviceIdArray.size(); ++i) {

					// access element as v[i]
					logt("ALARM_SYSTEM", "LOOP: INDEX %u -> positionId %u temp %u hum %u",
							i,
							meshDeviceIdArray.at(i),
							meshDataOneArray.at(i),
							meshDataTwoArray.at(i));
					if (meshDeviceIdArray.at(i) == data->meshDeviceId) {
						index = i;
						logt("ALARM_SYSTEM", "ID found at Index %u ", index);
						//if the id is found, jump out of the loop
						break;
					}
				}
				logt("ALARM_SYSTEM", "ID found at Index outside %u ", index);

				if (data->meshActionType == AlarmModuleStates::DELETE) {
					meshDeviceIdArray.erase(std::remove(meshDeviceIdArray.begin(), meshDeviceIdArray.end(), data->meshDeviceId), meshDeviceIdArray.end());
					logt("ALARM_SYSTEM", "positionId ist at Position %u ", index);
					meshDataTypeArray.erase(meshDataTypeArray.begin() + (index));
					meshDataOneArray.erase(meshDataOneArray.begin() + (index));
					meshDataTwoArray.erase(meshDataTwoArray.begin() + (index));
					logt("ALARM_SYSTEM", "positionId %u removed", data->meshDeviceId);
					logt("ALARM_SYSTEM", "Temp Size %u ",meshDataOneArray.size() );
				} else if(data->meshActionType == AlarmModuleStates::SAVE) {
					meshDataTypeArray.at(index) = data->meshDataType;
					meshDataOneArray.at(index) = data->meshDataOne;
					meshDataTwoArray.at(index) = data->meshDataTwo;
					logt("ALARM_SYSTEM", "Temp updated %u, Hum updated %u", meshDataOneArray.at(index), meshDataTwoArray.at(index));

				}
			}
		}
		BroadcastAlarmAdvertisingPacket();
	}
};

#define ____________BAROMETER____________________

/**
 * Intialisation Sequence for Bosch BME280 for Pressure
 * Driver: bme280.c/.h
 * */
void AlarmModule::InitialiseBME280()
{
	ret_code_t err_code = NRF_SUCCESS;
	u8 ctrl_reg;

	if (!spi_isInitialized()) {
		spi_init(
				Boardconfig->spiM0SckPin,
				Boardconfig->spiM0MisoPin,
				Boardconfig->spiM0MosiPin,
				Boardconfig->spiM0SSAccPin,
				Boardconfig->spiM0SSBmePin
		);
	}

	err_code = bme280_init();
	//logt("ALARM_SYSTEM","bme280_init() returns: %x",err_code);
	nrf_delay_ms(10);
	//
	bme280_set_mode_assert(BME280_MODE_SLEEP);

	//We use oversampling with 2 samples and an IIR that accumulates 2 samples
	//This should give fast responses (about 15ms measurement time) with RMS noise 1.5 in pressure
	err_code = bme280_set_oversampling_press(BME280_OVERSAMPLING_2);
	err_code |= bme280_set_oversampling_temp(BME280_OVERSAMPLING_2);
	err_code = bme280_set_oversampling_hum(BME280_OVERSAMPLING_2);
	err_code |= bme280_set_iir(BME280_IIR_8);
	err_code |= bme280_set_interval(BME280_STANDBY_1000_MS);
	//Do the first measurement (we have to wait 11ms until it is acquired)
	bme280_set_mode_assert(BME280_MODE_NORMAL);

	uint8_t data1;
	data1 = bme280_read_reg(BME280REG_CTRL_MEAS);
	//logt("ALARM_SYSTEM","BME280REG_CTRL_MEAS: %x ",data1 );

}

void AlarmModule::UpdateBarometerData()
{
	if(!configuration.enableBarometer) return;

	//If the barometer value was read recently, we return the last read value
	if(lastBarometerReadTimeDs != 0 && lastBarometerReadTimeDs + ALARM_MODULE_BAROMETER_SLEEP_DS > GS->node->appTimerDs)
	{
		return;
	}

	BME280_Ret err = bme280_read_measurements();

	if(err != BME280_RET_OK) {
		logt("ERROR"," bme280_read_measurements failed with %d", err);
		return;
	}
	//if(lastTemperatureReading != (bme280_get_temperature() / 100)){
	//	BroadcastAlarmUpdatePacket();
	//}
	lastTemperatureReading = bme280_get_temperature() / 100;//Full degree celcius
	lastHumidityReading = bme280_get_humidity() / 1024;//In full %RH

	logt("ALARM_SYSTEM"," bme280_read_measurement %u, %u", lastTemperatureReading, lastHumidityReading);

	lastBarometerReadTimeDs = GS->node->appTimerDs;

	return;
}

void AlarmModule::TimerEventHandler(u16 passedTimeDs, u32 appTimerDs) {
	if (!configuration.moduleActive)
	return;

	if(SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, ALARM_MODULE_BROADCAST_TRIGGER_TIME)) {
		BroadcastAlarmAdvertisingPacket();
	}

	if(SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, ALARM_MODULE_SENSOR_SCAN_TRIGGER_TIME)) {
		nrf_gpio_pin_set(PIN_OUT);
		logt("ALARM_SYSTEM"," Pin High ");
		logt("ALARM_SYSTEM"," Window state  %u", nrf_gpio_pin_read(PIN_IN));

		if(nrf_gpio_pin_read(PIN_IN) != gpioState) {
			GS->ledRed->On();
			gpioState = nrf_gpio_pin_read(PIN_IN);
			BroadcastAlarmUpdatePacket();
			logt("ALARM_SYSTEM"," Window state changed to %u", nrf_gpio_pin_read(PIN_IN));
			GS->ledRed->Off();
		}
		nrf_gpio_pin_clear(PIN_OUT);
		//nrf_gpio_pin_set(PIN_OUT);
		logt("ALARM_SYSTEM"," Pin LOW ");

		//BackUp For 2 Cluster
		if(lastClusterSize != GS->node->clusterSize) {
			BroadcastAlarmUpdatePacket();
			lastClusterSize = GS->node->clusterSize;
			logt("ALARM_SYSTEM"," Clustersize changed to  %u",GS->node->clusterSize);
		}
	}

	if(gpioState == AlarmModuleStates::SAVE &&
			(i8)lastTemperatureReading != stateChangedTemperatureReading) {
		BroadcastAlarmUpdatePacket();
		stateChangedTemperatureReading = lastTemperatureReading;
		logt("ALARM_SYSTEM"," Temperature changed from %u to %u",stateChangedTemperatureReading,(i8)lastTemperatureReading);
	}
}

void AlarmModule::GpioInit() {
	nrf_gpio_pin_dir_set(PIN_OUT, NRF_GPIO_PIN_DIR_OUTPUT);
	nrf_gpio_cfg_output(PIN_OUT);
	nrf_gpio_pin_set(PIN_OUT);
	nrf_gpio_cfg_input(PIN_IN, NRF_GPIO_PIN_NOPULL);
}

#endif

