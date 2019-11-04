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

#define SERVICE_DATA_MESSAGE_TYPE_ASSET 0x02

#pragma pack(push)
#pragma pack(1)

//Service Data (max. 24 byte)
#define SIZEOF_ADV_STRUCTURE_ASSET_SERVICE_DATA 24
typedef struct
{
	//6 byte header
	u8 len;
	u8 type;
	u16 uuid;
	u16 messageType; //0x02 for Asset Service

	//1 byte capabilities
	u8 advertisingChannel : 2; // 0 = not available, 1=37, 2=38, 3=39
	u8 gyroscopeAvailable : 1;
	u8 magnetometerAvailable : 1;
	u8 reservedBit : 4;

	//11 byte assetData
	u32 serialNumberIndex;
	u8 batteryPower; //0xFF = not available
	u8 speed; //0xFF = not available
	u8 direction; //0xFF = not available
	u16 pressure; //0xFFFF = not available
	i8 temperature; //0xFF = not available
	u8 humidity; //0xFF = not available

	u16 reserved;

	u32 encryptionMIC;


}advPacketAssetServiceDataMiro;

#pragma pack(pop)

#ifdef ACTIVATE_ASSET_MODULE

#include <AdvertisingController.h>

extern "C"{
#include <nrf_drv_gpiote.h>
}


typedef struct
{
	i16 x;
	i16 y;
	i16 z;
} ThreeDimStruct;

typedef struct
{
	ThreeDimStruct acc;
	ThreeDimStruct vel;
	u32 bar;
	u32 timeStamp;
} AssetModuleMeshMessage;


class AssetModule: public Module
{
private:
	//Module configuration that is saved persistently (size must be multiple of 4)
	struct AssetModuleConfiguration: ModuleConfiguration
	{
		//Insert more persistent config values here
		float wakeupThreshold;
		u16 wakeupDuration;
		u16 movementEndThresholdMilliG; //If below this threshold for a number of steps, movement will end
		u16 movementEndDelayDs; // Number of steps to wait after no movement was detected
		u8 enableAccelerometer;
		u8 enableBarometer;
		u16 advIntervalMovingMs; //Advertising interval during movement
		u16 advIntervalSleepMs; //Advertising interval during standstill

	};

	enum AssetModuleActionResponseMessages
	{

	};

	AdvJob* assetJobHandle;

	//Use for movement end detection
	u32 lastMovementTimeDs;

	u8 currentAdvChannel;

	ThreeDimStruct prev_acc;
	ThreeDimStruct currentAcc;
	ThreeDimStruct vel;
	bool moving = false;

	//barometer
	u32 lastBarometerReadTimeDs;
	u32 lastPressureReading;
	i32 lastTemperatureReading;
	u32 lastHumidityReading;


	void InitialiseLis2dh12();
	ret_code_t ConfigureLis2dh12forStandby();
	ret_code_t ConfigureLis2dh12forMeasurement();
	void HandleLis2dh12Interrupt();

	void addValuesToCircular(i32 x, i32 y, i32 z);
	void refreshVariancesCircular();
	void MovementEndDetection(ThreeDimStruct* vel, ThreeDimStruct* acc);
	void CalculateVariances();
	void CalculateAverage();

	void calibration();
	void CalculateVelocityFromAcceleration(ThreeDimStruct * acc, ThreeDimStruct * prev_acc, ThreeDimStruct * vel);
	void copyThreeDimStuct(ThreeDimStruct * into, ThreeDimStruct * from);
	void GetAccelerationData(ThreeDimStruct * acc);

	u8 CalcEffectiveVelocity(ThreeDimStruct * vel);
	void MovementEndHandler();
	u8 ConvertVelocityToADVFormat(ThreeDimStruct * vel);

	u32 EnableLis2dh12Pin1Interrupt(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t polarity, nrf_drv_gpiote_evt_handler_t handler);

	void InitialiseBME280();
	void UpdateBarometerData();


	void SendAssetDataToMesh(ThreeDimStruct * acc, ThreeDimStruct * vel, u32 appTimerDs,u32 bar);

	void printArray(const char * preamble, u8 * ptr, u8 len);

public:


	DECLARE_CONFIG_AND_PACKED_STRUCT (AssetModuleConfiguration);

	static bool lis2dh12WakeupInterruptWasTriggered;

	AssetModule(u8 moduleId, Node* node, ConnectionManager* cm,
			const char* name);

	void ConfigurationLoadedHandler();
    void GetLinearAcceleration(ThreeDimStruct * acc, ThreeDimStruct * linear_acc);
	void ResetToDefaultConfiguration();

	void BroadcastAssetAdvertisingPacket(u16 advIntervalMs);

	void MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader);

	void TimerEventHandler(u16 passedTimeDs, u32 appTimerDs);

	bool TerminalCommandHandler(char* commandArgs[], u8 commandArgsSize);

	void UpdateAssetDataAdvPacket(u16 advertisingIntervalinMs, u8 accelerometerData, u8 barometerData);

	void GetTilt(ThreeDimStruct * acc);

	static void Lis2dh12Int1Handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action);
};

#endif
