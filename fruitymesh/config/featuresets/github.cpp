////////////////////////////////////////////////////////////////////////////////
// /****************************************************************************
// **
// ** Copyright (C) 2015-2019 M-Way Solutions GmbH
// ** Contact: https://www.blureange.io/licensing
// **
// ** This file is part of the Bluerange/FruityMesh implementation
// **
// ** $BR_BEGIN_LICENSE:GPL-EXCEPT$
// ** Commercial License Usage
// ** Licensees holding valid commercial Bluerange licenses may use this file in
// ** accordance with the commercial license agreement provided with the
// ** Software or, alternatively, in accordance with the terms contained in
// ** a written agreement between them and M-Way Solutions GmbH.
// ** For licensing terms and conditions see https://www.bluerange.io/terms-conditions. For further
// ** information use the contact form at https://www.bluerange.io/contact.
// **
// ** GNU General Public License Usage
// ** Alternatively, this file may be used under the terms of the GNU
// ** General Public License version 3 as published by the Free Software
// ** Foundation with exceptions as appearing in the file LICENSE.GPL3-EXCEPT
// ** included in the packaging of this file. Please review the following
// ** information to ensure the GNU General Public License requirements will
// ** be met: https://www.gnu.org/licenses/gpl-3.0.html.
// **
// ** $BR_END_LICENSE$
// **
// ****************************************************************************/
////////////////////////////////////////////////////////////////////////////////
#include "Config.h"
#include "Node.h"
#include "Utility.h"
#include "DebugModule.h"
#include "StatusReporterModule.h"
#include "AdvertisingModule.h"
#include "ScanningModule.h"
#include "EnrollmentModule.h"
#include "IoModule.h"
#include "MeshAccessModule.h"
#include "GlobalState.h"
#include "AlarmModule.h"
#include "AssetModule.h"

#define UART_BAUDRATE_BAUDRATE_Baud1M (0x10000000UL) /*!< 1Mega baud */
enum class ClockSource : u8
{
	CLOCK_SOURCE_RC = 0,
	CLOCK_SOURCE_XTAL = 1,
	CLOCK_SOURCE_SYNTH = 2
};

enum class ClockAccuracy : u8
{
	CLOCK_ACCURACY_250_PPM = 0, //Default
	CLOCK_ACCURACY_500_PPM = 1,
	CLOCK_ACCURACY_150_PPM = 2,
	CLOCK_ACCURACY_100_PPM = 3,
	CLOCK_ACCURACY_75_PPM = 4,
	CLOCK_ACCURACY_50_PPM = 5,
	CLOCK_ACCURACY_30_PPM = 6,
	CLOCK_ACCURACY_20_PPM = 7,
	CLOCK_ACCURACY_10_PPM = 8,
	CLOCK_ACCURACY_5_PPM = 9,
	CLOCK_ACCURACY_2_PPM = 10,
	CLOCK_ACCURACY_1_PPM = 11,
};

void setFeaturesetConfiguration_github(ModuleConfiguration *config, void *module)
{
	if (config->moduleId == ModuleId::BOARD_CONFIG)
	{
		BoardConfiguration *c = (BoardConfiguration *)config;
		c->led1Pin = 18;
		c->led2Pin = 19;
		c->led3Pin = 17;
		c->ledActiveHigh = true;
		c->button1Pin = 13;
		c->buttonsActiveHigh = false;
		c->uartRXPin = -1;
		c->uartTXPin = -1;
		c->uartCTSPin = -1;
		c->uartRTSPin = -1;
		c->uartBaudRate = UART_BAUDRATE_BAUDRATE_Baud1M;
		c->dBmRX = -96;
		c->calibratedTX = -55;
		c->lfClockSource = (u8)ClockSource::CLOCK_SOURCE_XTAL;
		c->lfClockAccuracy = (u8)ClockAccuracy::CLOCK_ACCURACY_30_PPM; //10ppm confirmed by e-mail
		c->dcDcEnabled = true;
		// Use chip input voltage measurement
		c->batteryAdcInputPin = -2;
	}
	else if (config->moduleId == ModuleId::CONFIG)
	{
		Conf::getInstance().defaultLedMode = LedMode::CONNECTIONS;
		Conf::getInstance().terminalMode = TerminalMode::PROMPT;
	}
	else if (config->moduleId == ModuleId::NODE)
	{
		//Specifies a default enrollment for the github configuration
		//This enrollment will be overwritten as soon as the node is either enrolled or the enrollment removed
		NodeConfiguration *c = (NodeConfiguration *)config;
		c->enrollmentState = EnrollmentState::ENROLLED;
		// network id has to be the same for all devices
		c->networkId = 11;
		// nodeId to use for the devices to flash
		c->nodeId = 17;
		c->direction = 8;
		c->boardType = 1;
		c->checkDirection = true;
		CheckedMemset(c->networkKey, 0x00, 16);
	}
}

u32 initializeModules_github(bool createModule)
{
	u32 size = 0;
	size += GS->InitializeModule<DebugModule>(createModule);
	size += GS->InitializeModule<StatusReporterModule>(createModule);
	size += GS->InitializeModule<AdvertisingModule>(createModule);
	size += GS->InitializeModule<ScanningModule>(createModule);
	size += GS->InitializeModule<EnrollmentModule>(createModule);
	size += GS->InitializeModule<IoModule>(createModule);
	size += GS->InitializeModule<MeshAccessModule>(createModule);
	size += GS->InitializeModule<AssetModule>(createModule);
	size += GS->InitializeModule<AlarmModule>(createModule);

	return size;
}

DeviceType getDeviceType_github()
{
	return DeviceType::STATIC;
}
