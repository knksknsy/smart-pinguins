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

#include <AssetModule.h>

#ifdef ACTIVATE_ASSET_MODULE

#include <Logger.h>
#include <StatusReporterModule.h>
#include <MeshAccessModule.h>
#include <Utility.h>
#include <Node.h>

extern "C"{
#ifdef NRF52
#include <lis2dh12.h>
#include "nrf_drv_spi.h"
#include "bme280.h"
#endif

#include "nrf_drv_gpiote.h"
#include "nrf_delay.h"
#include "math.h"
}

#define ASSET_MODULE_BAROMETER_SLEEP_DS 50
#define ASSET_MODULE_ENCRYPT_ADV_DATA false
#define ASSET_MODULE_SLEEP_ADV_UPDATE_TIME_DS 50


bool AssetModule::lis2dh12WakeupInterruptWasTriggered = false;

AssetModule::AssetModule(u8 moduleId, Node* node, ConnectionManager* cm, const char* name)
: Module(moduleId, node, cm, name)
{
	//Register callbacks n' stuff
	moduleVersion = 1;
	//Save configuration to base class variables
	//sizeof configuration must be a multiple of 4 bytes
	configurationPointer = &configuration;
	configurationLength = sizeof(AssetModuleConfiguration);

	assetJobHandle = NULL;

	lis2dh12WakeupInterruptWasTriggered = false;

	lastPressureReading = 0;
	lastTemperatureReading = 0;
	lastHumidityReading = 0;
	lastBarometerReadTimeDs = 0;

	lastMovementTimeDs = 0;

	moving = true;
	currentAdvChannel = 0;

	//Start module configuration loading
	LoadModuleConfiguration();
}

void AssetModule::ResetToDefaultConfiguration()
{
	//Set default configuration values
	configuration.moduleId = moduleId;
	configuration.moduleActive = true;
	configuration.moduleVersion = 1;

	//Set additional config values...
	configuration.wakeupThreshold = 0.1;
	configuration.wakeupDuration = 500;
	configuration.movementEndThresholdMilliG = 15;
	configuration.movementEndDelayDs = 450;
	configuration.enableAccelerometer = true;
	configuration.enableBarometer = true;
	configuration.advIntervalMovingMs = 100;
	configuration.advIntervalSleepMs = 1000;
}

void AssetModule::ConfigurationLoadedHandler()
{
	//Does basic testing on the loaded configuration
	Module::ConfigurationLoadedHandler();

	//Version migration can be added here
	if (configuration.moduleVersion == this->moduleVersion)
	{

	};

	if(configuration.moduleActive && GS->node->configuration.networkId != 0){
#ifdef NRF52
		//Enable accelerometer if available
		if(Boardconfig->spiM0SSAccPin != -1 && configuration.enableAccelerometer){
			InitialiseLis2dh12();
			this->ConfigureLis2dh12forMeasurement();
		}

		//Enable barometer if available
		if(Boardconfig->spiM0SSBmePin != -1 && configuration.enableBarometer){
			InitialiseBME280();
		}
#endif

		//Start broadcasting at high interval, will be disabled if accelerometer detects no movement
		BroadcastAssetAdvertisingPacket(configuration.advIntervalMovingMs);

		//Disable MeshAccessModule Broadcasting job to spend all resources on asset advertising
		MeshAccessModule* maMod = (MeshAccessModule*)GS->node->GetModuleById(moduleID::MESH_ACCESS_MODULE_ID);
		if(maMod != NULL){
			maMod->DisableBroadcast();
		}
	}
}



void AssetModule::TimerEventHandler(u16 passedTimeDs, u32 appTimerDs)
{
	if(!configuration.moduleActive || GS->node->configuration.networkId == 0) return;

#ifdef NRF52
	if (lis2dh12WakeupInterruptWasTriggered == true){
		HandleLis2dh12Interrupt();
	}
#endif

	if (this->moving)
	{
		if(configuration.enableAccelerometer && Boardconfig->spiM0SSAccPin != -1){
#ifdef NRF52
			//Read data from accelerometer
			this->GetAccelerationData(&currentAcc);
			//Calculate velocity
			this->CalculateVelocityFromAcceleration(&currentAcc, &(this->prev_acc), &(this->vel));
			//Updates the asset packet
			BroadcastAssetAdvertisingPacket(configuration.advIntervalMovingMs);
			//Checks if the movement stopped and goes to sleep again
			MovementEndDetection(&(this->vel), &this->currentAcc);
#endif
		} else if(SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, SEC_TO_DS(1))){
			//Updates the asset packet all the time if no accelerometer is present
			//FIXME: only needed for different channel mask currently, otherwise no need to update
			BroadcastAssetAdvertisingPacket(configuration.advIntervalMovingMs);
		}
	} else {
		//We must also update barometer readings and stuff during sleep
		if(SHOULD_IV_TRIGGER(appTimerDs, passedTimeDs, ASSET_MODULE_SLEEP_ADV_UPDATE_TIME_DS)){
			BroadcastAssetAdvertisingPacket(configuration.advIntervalSleepMs);
		}
	}
}

void AssetModule::BroadcastAssetAdvertisingPacket(u16 advIntervalMs)
{
	currentAdvChannel = Utility::GetRandomInteger() % 3;

	//build advertising packet
	AdvJob job = {
		AdvJobTypes::ADV_JOB_TYPE_SCHEDULED, //JobType
		5, //Slots
		0, //Delay
		MSEC_TO_UNITS(100, UNIT_0_625_MS), //AdvInterval
		0, //AdvChannel
		0, //CurrentSlots
		0, //CurrentDelay
		BLE_GAP_ADV_TYPE_ADV_IND, //Advertising Mode
		{0}, //AdvData
		0, //AdvDataLength
		{0}, //ScanData
		0 //ScanDataLength
	};

	//Select either the new advertising job or the already existing
	AdvJob* currentJob;
	if(assetJobHandle == NULL){
		currentJob = &job;
	} else {
		currentJob = assetJobHandle;
	}
	u8* bufferPointer = currentJob->advData;

	//Update the advertising interval for our job
	currentJob->advertisingInterval = MSEC_TO_UNITS(advIntervalMs, UNIT_0_625_MS);
	currentJob->advertisingChannelMask = 0x07 ^ (1 << currentAdvChannel); //Rotate adv channel

	advStructureFlags* flags = (advStructureFlags*)bufferPointer;
	flags->len = SIZEOF_ADV_STRUCTURE_FLAGS-1; //minus length field itself
	flags->type = BLE_GAP_AD_TYPE_FLAGS;
	flags->flags = BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE | BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED;

	advStructureUUID16* serviceUuidList = (advStructureUUID16*)(bufferPointer+SIZEOF_ADV_STRUCTURE_FLAGS);
	serviceUuidList->len = SIZEOF_ADV_STRUCTURE_UUID16 - 1;
	serviceUuidList->type = BLE_GAP_AD_TYPE_16BIT_SERVICE_UUID_COMPLETE;
	serviceUuidList->uuid = SERVICE_DATA_SERVICE_UUID16;

	advPacketAssetServiceData* serviceData = (advPacketAssetServiceData*)(bufferPointer+SIZEOF_ADV_STRUCTURE_FLAGS+SIZEOF_ADV_STRUCTURE_UUID16);
	serviceData->len = SIZEOF_ADV_STRUCTURE_ASSET_SERVICE_DATA - 1;
	serviceData->type = BLE_GAP_AD_TYPE_SERVICE_DATA;
	serviceData->uuid = SERVICE_DATA_SERVICE_UUID16;
	serviceData->messageType = SERVICE_DATA_MESSAGE_TYPE_ASSET;

	serviceData->serialNumberIndex = RamConfig->serialNumberIndex;

	serviceData->advertisingChannel = currentAdvChannel + 1;

	//FIXME: Use proper values
	serviceData->magnetometerAvailable = false;
	serviceData->gyroscopeAvailable = false;

	serviceData->batteryPower = 1; //FIXME: Use correct value, only measure frome time to time

	if(configuration.enableAccelerometer && Boardconfig->spiM0SSAccPin != -1){
#ifdef NRF52
		serviceData->speed = CalcEffectiveVelocity(&(this->vel));
		logt("ERROR", "speed normal %u", serviceData->speed);

		//If there was movement in the last 4 seconds, we transmit 1 instead of no speed
		//Afterwards we transmit 0
		if(lastMovementTimeDs + 40 > GS->node->appTimerDs && serviceData->speed < 1){
			serviceData->speed = 1;
		} else if(lastMovementTimeDs + 40 < GS->node->appTimerDs){
			serviceData->speed = 0;
		}
#endif
	} else {
		serviceData->speed = 0xFF; //Not available
	}

	logt("ASMOD", "speed after %u", serviceData->speed);

	serviceData->direction = 0xFF;//TODO: Fill with other values

	if(configuration.enableBarometer && Boardconfig->spiM0SSBmePin != -1){
#ifdef NRF52
		UpdateBarometerData();

		serviceData->pressure = (u16)lastPressureReading;
		serviceData->temperature = (i8)lastTemperatureReading;
		serviceData->humidity = (u8)lastHumidityReading; //TODO: Check if u8 is enough
#endif
	} else {
		serviceData->pressure = 0xFFFF;
		serviceData->temperature = 0xFF;
		serviceData->humidity = 0xFF;
	}

	logt("ASMOD","channel %u, velocity %u, pressure: %d", currentAdvChannel, serviceData->speed, serviceData->pressure);

	//
	if(ASSET_MODULE_ENCRYPT_ADV_DATA)
	{
		//TODO: Generate keystream from asset key and current timer value (divided so it changes all 10 seconds e.g.)

		//TODO: Calculate MIC
	}

	u32 length = SIZEOF_ADV_STRUCTURE_FLAGS + SIZEOF_ADV_STRUCTURE_UUID16 + SIZEOF_ADV_STRUCTURE_ASSET_SERVICE_DATA;
	job.advDataLength = length;

	//Either update the job or create it if not done
	if(assetJobHandle == NULL){
		assetJobHandle = AdvertisingController::getInstance()->AddJob(&job);
	} else {
		AdvertisingController::getInstance()->RefreshJob(assetJobHandle);
	}


	char cbuffer[100];
	Logger::getInstance()->convertBufferToHexString(currentJob->advData, length, cbuffer, 100);
	logt("ASMOD", "Broadcasting asset data %s, len %u", cbuffer, length);

}

void AssetModule::MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader)
{

}

bool AssetModule::TerminalCommandHandler(char* commandArgs[], u8 commandArgsSize)
{
	if(commandArgsSize >= 3 && TERMARGS(2, moduleName))
	{

	}

	//Must be called to allow the module to get and set the config
	return Module::TerminalCommandHandler(commandArgs, commandArgsSize);
}

#define ____________BAROMETER____________________
#ifdef NRF52
/**
 * Intialisation Sequence for Bosch BME280 for Pressure
 * Driver: bme280.c/.h
 * */
void  AssetModule::InitialiseBME280()
{
	ret_code_t err_code = NRF_SUCCESS;
	u8 ctrl_reg;

	if (!spi_isInitialized()){
		spi_init(
			Boardconfig->spiM0SckPin,
			Boardconfig->spiM0MisoPin,
			Boardconfig->spiM0MosiPin,
			Boardconfig->spiM0SSAccPin,
			Boardconfig->spiM0SSBmePin
		);
	}

	err_code = bme280_init();
	logt("ASMOD","bme280_init() returns: %x",err_code);
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
	logt("ASMOD","BME280REG_CTRL_MEAS: %x ",data1 );

}

/**
 * Measurement Sequence for Bosch BME280 for Pressure
 * Division by 2560 means that this function returns 1/10th hPa (deci hPa, 10 Pa)
 * 	- will change about 0.8 deci hPa for 1m altitude in air
 *  - will change about 1000 deci hPa for 1m altitude in water
 * Driver: bme280.c/.h
 * */
void AssetModule::UpdateBarometerData()
{
	if(!configuration.enableBarometer) return;

	//If the barometer value was read recently, we return the last read value
	if(lastBarometerReadTimeDs != 0 && lastBarometerReadTimeDs + ASSET_MODULE_BAROMETER_SLEEP_DS > GS->node->appTimerDs)
	{
		return;
	}

	BME280_Ret err = bme280_read_measurements();

	if(err != BME280_RET_OK) {
		logt("ERROR"," bme280_read_measurements failed with %d", err);
		return;
	}

	lastPressureReading = bme280_get_pressure() / 2560; // 1/10th of a hPa or in 10 Pascal steps
	lastTemperatureReading = bme280_get_temperature() / 100; //Full degree celcius
	lastHumidityReading = bme280_get_humidity() / 1024; //In full %RH

	logt("ASMOD"," bme280_read_measurement %u, %u, %u", lastPressureReading, lastTemperatureReading, lastHumidityReading);

	lastBarometerReadTimeDs = GS->node->appTimerDs;

	return;
}

#define ____________ACCELEROMETER____________________

void AssetModule::InitialiseLis2dh12()
{
	u32 err = 0;

	// initialize variables
	this->prev_acc = {0,0,0};
	this->currentAcc = {0,0,0};
	this->vel = {0,0,0};

	if (!spi_isInitialized()){
		spi_init(
			Boardconfig->spiM0SckPin,
			Boardconfig->spiM0MisoPin,
			Boardconfig->spiM0MosiPin,
			Boardconfig->spiM0SSAccPin,
			Boardconfig->spiM0SSBmePin
		);
	}

	// initialize gpio and spi
	if(lis2dh12_initialise() == NRF_SUCCESS)
	logt("ASMOD","LIS2DH12 Driver Initialised");//this check for SPI initialisation only!
	lis2dh12_reset();
	nrf_delay_ms(10);
	lis2dh12_enable();

	err = EnableLis2dh12Pin1Interrupt(Boardconfig->lis2dh12Interrupt1Pin, NRF_GPIOTE_POLARITY_TOGGLE, AssetModule::Lis2dh12Int1Handler);

	//TODO: Check error code and handle errors

	lis2dh12_set_fifo_mode(LIS2DH12_MODE_BYPASS);
	lis2dh12_set_scale(LIS2DH12_SCALE2G);
	lis2dh12_set_resolution(LIS2DH12_RES12BIT);
	lis2dh12_set_sample_rate(LIS2DH12_RATE_100);

	uint8_t ctrl1[1]={0};
	lis2dh12_set_mode(LIS2DH12_HIGH_RESOLUTION_MODE);//1&4
	lis2dh12_write_reg2(0x09);

	ConfigureLis2dh12forStandby();
}

u32 AssetModule::ConfigureLis2dh12forStandby()
{
	u32 err;
	uint8_t ctrl1[1]={0};
	lis2dh12_set_data_rate(LIS2DH12_ODR_MASK_10HZ);
	//nrf_delay_ms(10);

	lis2dh12_set_sleep_to_wake_threshold(configuration.wakeupThreshold);
	err = lis2dh12_set_interrupts(LIS2DH12_I1_AOI);
	uint8_t ctrl5 =  lis2dh12_set_latch();
	lis2dh12_set_hlactive();
	lis2dh12_set_sleep_to_wake_duration(LIS2DH12_HIGH_RESOLUTION_MODE,
			LIS2DH12_ODR_MASK_10HZ, configuration.wakeupDuration);
	err |= lis2dh12_set_int1_ths(configuration.wakeupThreshold);
	err |= lis2dh12_set_int1_duration(0x00);
	err |= lis2dh12_set_int1_cfg(LIS2DH12_ZHIE_MASK | LIS2DH12_YHIE_MASK | LIS2DH12_XHIE_MASK);
//	logt("ERROR","Standby mode configured");
	lis2dh12_ret_t err2 = lis2dh12_read_int1_src(ctrl1, 1);

	//TODO: handle err2?

	return err;
}

u32 AssetModule::ConfigureLis2dh12forMeasurement()
{
	u32 err;
	err = lis2dh12_reset_act_dur();
	err |= lis2dh12_reset_act_ths();

	//Clear INT1
	uint8_t ctrl1[1]={0};
	lis2dh12_ret_t err2 = lis2dh12_read_int1_src(ctrl1, 1);

	return err;
}

// Uses current acceration and checks if it is below configured threshold
// if it is below the threshold for a certain period, the movement ends
void AssetModule::MovementEndDetection(ThreeDimStruct* vel, ThreeDimStruct* acc)
{
	if(!moving) return;

	//If we are moving, reset the lastMovement Time to current time
	if(
		acc->x > configuration.movementEndThresholdMilliG
		|| acc->y > configuration.movementEndThresholdMilliG
		|| acc->z > configuration.movementEndThresholdMilliG
	){
		lastMovementTimeDs = GS->node->appTimerDs;
	}

	//If we haven't moved longer than the delay, we stop
	if(lastMovementTimeDs + configuration.movementEndDelayDs < GS->node->appTimerDs){
		vel->x = 0;
		vel->y = 0;
		vel->z = 0;
		lastMovementTimeDs = 0;

		MovementEndHandler();
	}
}

void AssetModule::MovementEndHandler()
{
	//Configures the advertising to a low interval
	BroadcastAssetAdvertisingPacket(configuration.advIntervalSleepMs);

	//Resets clear register and configures lis2dh12 to go to standby
	uint8_t ctrl[1];
	lis2dh12_ret_t err = lis2dh12_read_int1_src(ctrl, 1);
	this->moving = false;
	this->ConfigureLis2dh12forStandby();
}

/** Returns current velocity: Numerical Integration Calculation **/
void AssetModule::CalculateVelocityFromAcceleration(ThreeDimStruct * acc, ThreeDimStruct * prev_acc, ThreeDimStruct * vel)
{
	vel->x = (vel->x + (acc->x)*MAIN_TIMER_TICK_DS)/10;
	vel->y = (vel->y + (acc->y)*MAIN_TIMER_TICK_DS)/10;
	vel->z = (vel->z + (acc->z)*MAIN_TIMER_TICK_DS)/10;
}

#define ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS 1

void AssetModule::GetAccelerationData(ThreeDimStruct * acc)
{
	i32 x = 0, y = 0, z = 0;
	for (uint8_t i = 0; i < ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS; i++)
	{
		if(ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS > 1) nrf_delay_ms(1);
		lis2dh12_sensor_buffer_t buffer;
		lis2dh12_read_acceleration_samples(&buffer, 1);
		x = buffer.sensor.x + x;
		y = buffer.sensor.y + y;
		z = buffer.sensor.z + z;
	}

	x = x / ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS;
	y = y / ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS;
	z = z / ASSET_MODULE_NUM_ACCELEROMETER_MEASUREMENTS;

	acc->x = x;
	acc->y = y;
	acc->z = z;
}

//void AssetModule::GetTilt(ThreeDimStruct * acc)
//{
//	i32 pitch = 0, roll=0;
//	  roll = atan2(acc->x , acc->z) * 57.3;
//	  pitch = atan2((-acc->x) , sqrt(acc->y * acc->y + acc->z * acc->z)) * 57.3;
//
//	  logt("ERROR", "roll:%d , pitch:%d ",roll, pitch);
//}


/**
 *  Enable interrupt on pin. Pull-up is enabled on HITOLOW, pull-down is enabled on LOWTIHI
 *  Polarity can be defined:
 *  NRF_GPIOTE_POLARITY_LOTOHI
 *  NRF_GPIOTE_POLARITY_HITOLO
 *  NRF_GPIOTE_POLARITY_TOGGLE
 *
*/
u32 AssetModule::EnableLis2dh12Pin1Interrupt(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t polarity, nrf_drv_gpiote_evt_handler_t handler)
{
	u32 err = NRF_SUCCESS;
	nrf_drv_gpiote_in_config_t in_config;
	in_config.is_watcher = false;
	in_config.hi_accuracy = false;
	in_config.pull = NRF_GPIO_PIN_NOPULL;
	in_config.sense = polarity;

	switch(polarity)
	{
	case NRF_GPIOTE_POLARITY_TOGGLE:
		in_config.pull = NRF_GPIO_PIN_NOPULL;
		break;
	case NRF_GPIOTE_POLARITY_HITOLO:
		in_config.pull = NRF_GPIO_PIN_PULLUP;
		break;
	case NRF_GPIOTE_POLARITY_LOTOHI:
		in_config.pull = NRF_GPIO_PIN_PULLDOWN;
		break;
	default:
		return 1;
	}
	err |= nrf_drv_gpiote_in_init(pin, &in_config, handler);
	nrf_drv_gpiote_in_event_enable(pin, true);

	return err;
}

/**
 *  Handle interrupt from lis2dh12
 *  Never do long actions, such as sensor reads in interrupt context.
 *  Using peripherals in interrupt is also risky, as peripherals might require interrupts for their function.
 *
 *  @param message Ruuvi message, with source, destination, type and 8 byte payload. Ignore for now.
 **/
void AssetModule::Lis2dh12Int1Handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action)
{
	lis2dh12WakeupInterruptWasTriggered = true;
}

//Called from the main context, handles the accelerometer interrupt
void AssetModule::HandleLis2dh12Interrupt()
{
	logt("ASMOD","handleInterrupt was called");
	lis2dh12WakeupInterruptWasTriggered = false;
	moving = true;
	lastMovementTimeDs = GS->node->appTimerDs;
	//clear the interrupt
	this->ConfigureLis2dh12forMeasurement();
}

//Calculates effective velocity from velX, velY and velZ
u8 AssetModule::CalcEffectiveVelocity(ThreeDimStruct * vel)
{
	u32 in_x = 0, in_y = 0, in_z = 0;

	in_x = (i32)(vel->x) * vel->x;
	in_y = (i32)(vel->y) * vel->y;
	in_z = (i32)(vel->z) * vel->z;

	double vel_cal = sqrt(in_x + in_y +in_z);

	if(vel_cal > 250) vel_cal = 250;

	u8 eff_vel = vel_cal < 0.1 ? 0 : (u8)vel_cal;

	return eff_vel;
}
#endif //NRF52

#define ____________HELPERS____________________

void AssetModule::printArray(const char * preamble, u8 * ptr, u8 len) {
	char str[3*(len+2)];
	Logger::getInstance()->convertBufferToHexString(ptr,len, str,3*(len+2));
	logt("ASMOD","%s: %s", preamble, str);
}

#endif	// ACTIVATE_ASSET_MODULE
