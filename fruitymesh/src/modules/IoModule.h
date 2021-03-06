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

#pragma once

#include <Module.h>

#pragma pack(push, 1)
//Module configuration that is saved persistently
struct IoModuleConfiguration : ModuleConfiguration {
	LedMode ledMode;
	//Insert more persistent config values here
};
STATIC_ASSERT_SIZE(IoModuleConfiguration, 5);
#pragma pack(pop)

class IoModule: public Module
{
	private:
		enum class IoModuleTriggerActionMessages : u8{
			SET_PIN_CONFIG = 0,
			GET_PIN_CONFIG = 1,
			GET_PIN_LEVEL = 2,
			SET_LED = 3 //used to trigger a signaling led
		};

		enum class IoModuleActionResponseMessages : u8{
			SET_PIN_CONFIG_RESULT = 0,
			PIN_CONFIG = 1,
			PIN_LEVEL = 2,
			SET_LED_RESPONSE = 3
		};

		//Combines a pin and its config
		static constexpr int SIZEOF_GPIO_PIN_CONFIG = 2;
		struct gpioPinConfig{
			u8 pinNumber : 5;
			u8 direction : 1; //configure pin as either input or output (nrf_gpio_pin_dir_t)
			u8 inputBufferConnected : 1; //disconnect input buffer when port not used to save energy
			u8 pull : 2; //pull down (1) or up (3) or disable pull (0) on pin (nrf_gpio_pin_pull_t)
			u8 driveStrength : 3; // GPIO_PIN_CNF_DRIVE_*
			u8 sense : 2; // if configured as input sense either high or low level
			u8 set : 1; // set pin or unset it
		};
		STATIC_ASSERT_SIZE(gpioPinConfig, 2);


		//####### Module messages (these need to be packed)
		#pragma pack(push)
		#pragma pack(1)

			static constexpr int SIZEOF_IO_MODULE_SET_LED_MESSAGE = 1;
			typedef struct
			{
				LedMode ledMode;

			}IoModuleSetLedMessage;
			STATIC_ASSERT_SIZE(IoModuleSetLedMessage, 1);

		#pragma pack(pop)
		//####### Module messages end

		u8 ledBlinkPosition = 0;

	public:

		DECLARE_CONFIG_AND_PACKED_STRUCT(IoModuleConfiguration);

		LedMode currentLedMode;

		IoModule();

		void ConfigurationLoadedHandler(ModuleConfiguration* migratableConfig, u16 migratableConfigLength) override;

		void ResetToDefaultConfiguration() override;

		void TimerEventHandler(u16 passedTimeDs) override;

		void MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader) override;

		#ifdef TERMINAL_ENABLED
		bool TerminalCommandHandler(char* commandArgs[], u8 commandArgsSize) override;
		#endif
};
