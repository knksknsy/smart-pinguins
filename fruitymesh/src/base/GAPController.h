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

/*
 * The GAP Controller wraps SoftDevice calls for initiating and accepting connections
 * It should also provide encryption in the future.
 */

#pragma once

#include "types.h"
#include "FruityHal.h"

class GAPController
{
public:
	static GAPController& getInstance();
	//Initialize the GAP module
	void bleConfigureGAP() const;

	//Connects to a peripheral with the specified address and calls the corresponding callbacks
	u32 connectToPeripheral(const fh_ble_gap_addr_t &address, u16 connectionInterval, u16 timeout) const;

	//Encryption
	void startEncryptingConnection(u16 connectionHandle) const;

	//Update the connection interval
	void RequestConnectionParameterUpdate(u16 connectionHandle, u16 minConnectionInterval, u16 maxConnectionInterval, u16 slaveLatency, u16 supervisionTimeout) const;



	//This handler is called with bleEvents from the softdevice
	void GapDisconnectedEventHandler(const GapDisconnectedEvent& disconnectEvent);
	void GapConnectedEventHandler(const GapConnectedEvent& connvectedEvent);
	void GapTimeoutEventHandler(const GapTimeoutEvent& gapTimeoutEvent);
	void GapSecurityInfoRequestEvenetHandler(const GapSecurityInfoRequestEvent& securityInfoRequestEvent);
	void GapConnectionSecurityUpdateEventHandler(const GapConnectionSecurityUpdateEvent& connectionSecurityUpdateEvent);
};

