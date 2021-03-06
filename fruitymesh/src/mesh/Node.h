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
 * The Node class is the heart and soul of this implementation. It uses a state
 * machine and timers to control the behaviour of the node.
 * It uses the FruityMesh algorithm to build up connections with surrounding nodes
 *
 */

#pragma once

#include <cstddef>
#include <types.h>
#include <stdbool.h>
#ifdef SIM_ENABLED
#include <SystemTest.h>
#endif
#include <LedWrapper.h>
#include <AdvertisingController.h>
#include <ScanController.h>
#include "MeshConnection.h"
#include <RecordStorage.h>
#include <Module.h>
#include <Terminal.h>
#include "SimpleArray.h"

constexpr int MAX_RAW_CHUNK_SIZE = 60;

enum class SensorMessageActionType : u8
{
    UNSPECIFIED = 0, // E.g. Generated by sensor itself
    ERROR_RSP = 1, // Error during READ/WRITE/...
    READ_RSP = 2, // Response following a READ
    WRITE_RSP = 3 // Response following a WRITE_ACK
};

enum class ActorMessageActionType : u8
{
    RESERVED = 0, // Unused
    WRITE = 1, // Write without acknowledgement
    READ = 2, // Read a value
    WRITE_ACK = 3 // Write with acknowledgement
};

enum class RawDataProtocol : u8
{
	UNSPECIFIED               = 0,
	HTTP                      = 1,
	GZIPPED_JSON              = 2,
	START_OF_USER_DEFINED_IDS = 200,
	LAST_ID                   = 255
};

enum class RawDataActionType : u8
{
	START          = 0,
	START_RECEIVED = 1,
	CHUNK          = 2,
	REPORT         = 3,
	ERROR_T        = 4
};

enum class RawDataErrorType : u8
{
	UNEXPECTED_END_OF_TRANSMISSION = 0,
	NOT_IN_A_TRANSMISSION          = 1,
	MALFORMED_MESSAGE              = 2,
	START_OF_USER_DEFINED_ERRORS   = 200,
	LAST_ID                        = 255
};

#pragma pack(push)
#pragma pack(1)
struct RawDataHeader
{
	connPacketHeader connHeader;
	ModuleId moduleId;
	u8 requestHandle;
	RawDataActionType actionType;
};

#define SIZEOF_RAW_DATA_LIGHT_PACKET (SIZEOF_CONN_PACKET_HEADER + 3)
struct RawDataLight
{
	connPacketHeader connHeader;
	ModuleId moduleId;
	u8 requestHandle;
	RawDataProtocol protocolId;

	u8 payload[1];
};
STATIC_ASSERT_SIZE(RawDataLight, SIZEOF_RAW_DATA_LIGHT_PACKET + 1);

struct RawDataStart
{
	RawDataHeader header;

	u32 numChunks : 24;
	u32 protocolId : 8; //RawDataProtocol
	u32 fmKeyId;
};
STATIC_ASSERT_SIZE(RawDataStart, 16);

struct RawDataStartReceived
{
	RawDataHeader header;
};
STATIC_ASSERT_SIZE(RawDataStartReceived, 8);

enum class RawDataErrorDestination : u8
{
	SENDER   = 1,
	RECEIVER = 2,
	BOTH     = 3
};

struct RawDataError
{
	RawDataHeader header;
	RawDataErrorType type;
	RawDataErrorDestination destination;
};
STATIC_ASSERT_SIZE(RawDataError, 10);

struct RawDataChunk
{
	RawDataHeader header;

	u32 chunkId : 24;
	u32 reserved : 8;
	u8 payload[1];
};
STATIC_ASSERT_SIZE(RawDataChunk, 13);
static_assert(offsetof(RawDataChunk, payload) % 4 == 0, "Payload should be 4 byte aligned!");

struct RawDataReport
{
	RawDataHeader header;
	u32 missings[3];
};
STATIC_ASSERT_SIZE(RawDataReport, 20);

#if defined(NRF52) || defined(SIM_ENABLED)
enum class CapabilityActionType : u8
{
	REQUESTED = 0,
	ENTRY = 1,
	END = 2
};

struct CapabilityHeader
{
	connPacketHeader header;
	CapabilityActionType actionType;
};

struct CapabilityRequestedMessage
{
	CapabilityHeader header;
};
STATIC_ASSERT_SIZE(CapabilityRequestedMessage, 6);

struct CapabilityEntryMessage
{
	CapabilityHeader header;
	u32 index;
	CapabilityEntry entry;
};
STATIC_ASSERT_SIZE(CapabilityEntryMessage, 128);

struct CapabilityEndMessage
{
	CapabilityHeader header;
	u32 amountOfCapabilities;
};
STATIC_ASSERT_SIZE(CapabilityEndMessage, 10);
#endif
#pragma pack(pop)

typedef struct
{
	u8         bleAddressType;  /**< See @ref BLE_GAP_ADDR_TYPES. */
	u8         bleAddress[BLE_GAP_ADDR_LEN];  /**< 48-bit address, LSB format. */
	GapAdvType advType;
	i8         rssi;
	u32        receivedTimeDs;
	u32        lastConnectAttemptDs;
	advPacketPayloadJoinMeV0 payload;
}joinMeBufferPacket;

//meshServiceStruct that contains all information about the meshService
typedef struct meshServiceStruct_temporary
{
	u16                     		serviceHandle;
	ble_gatts_char_handles_t		sendMessageCharacteristicHandle;
	ble_uuid_t						serviceUuid;
} meshServiceStruct;

#pragma pack(push)
#pragma pack(1)
	//Persistently saved configuration (should be multiple of 4 bytes long)
	//Serves to store settings that are changeable, e.g. by enrolling the node
	struct NodeConfiguration : ModuleConfiguration {
		i8 dBmTX_deprecated;
		fh_ble_gap_addr_t bleAddress; //7 bytes
		EnrollmentState enrollmentState;
		u8 deviceType_deprecated;
		NodeId nodeId;
		NetworkId networkId;
		u8 networkKey[16];
		u8 userBaseKey[16];
		u8 organizationKey[16];
		u8 direction;
		u8 boardType;
		bool checkDirection;
	};
#pragma pack(pop)

class Node: public Module
{

private:
		enum class NodeModuleTriggerActionMessages : u8
		{
			SET_DISCOVERY = 0,
			RESET_NODE = 1,
			SET_PREFERRED_CONNECTIONS = 2,
		};

		enum class NodeModuleActionResponseMessages : u8
		{
			SET_DISCOVERY_RESULT = 0,
			SET_PREFERRED_CONNECTIONS_RESULT = 2
		};

		bool stateMachineDisabled = false;

		u32 rebootTimeDs;

		void SendModuleList(NodeId toNode, u8 requestHandle) const;

		void SendRawError(NodeId receiver, ModuleId moduleId, RawDataErrorType type, RawDataErrorDestination destination, u8 requestHandle) const;
		bool CreateRawHeader(RawDataHeader* outVal, RawDataActionType type, char* commandArgs[], char* requestHandle) const;

		u32 ModifyScoreBasedOnPreferredPartners(u32 score, NodeId partner) const;
		

		//Incremented if other nodes are found that cannot connect to our cluster because we do not have
		//any more free outgoing connections
		u16 emergencyDisconnectCounter;

#if defined(NRF52) || defined(SIM_ENABLED)
		bool isSendingCapabilities = false;
		constexpr static u32 TIME_BETWEEN_CAPABILITY_SENDINGS_DS = SEC_TO_DS(1);
		u32 timeSinceLastCapabilitySentDs = 0;
		u32 capabilityRetrieverModuleIndex = 0;
		u32 capabilityRetrieverLocal = 0;
		u32 capabilityRetrieverGlobal = 0;
#endif
		bool isInit = false;
	public:
		DECLARE_CONFIG_AND_PACKED_STRUCT(NodeConfiguration);


		static constexpr int MAX_JOIN_ME_PACKET_AGE_DS = (10 * 10);
		static constexpr int JOIN_ME_PACKET_BUFFER_MAX_ELEMENTS = 10;
		SimpleArray<joinMeBufferPacket, JOIN_ME_PACKET_BUFFER_MAX_ELEMENTS> joinMePackets;
		ClusterId currentAckId;
		u16 connectionLossCounter;
		u16 randomBootNumber;

		AdvJob* meshAdvJobHandle;

		DiscoveryState currentDiscoveryState;
		DiscoveryState nextDiscoveryState;

		//Timers for state changing
		i32 currentStateTimeoutDs;
		u32 lastDecisionTimeDs;

		u8 noNodesFoundCounter; //Incremented every time that no interesting cluster packets are found

		//Variables (kinda private, but I'm too lazy to write getters)
		ClusterSize clusterSize;
		ClusterId clusterId;

		u32 radioActiveCount;

		bool outputRawData;

		bool initializedByGateway; //Can be set to true by a mesh gateway after all configuration has been set

		meshServiceStruct meshService;

		ScanJob * p_scanJob;

		bool isInBulkMode = false;

		// Result of the bestCluster calculation
		enum class DecisionResult : u8
		{
			CONNECT_AS_SLAVE, 
			CONNECT_AS_MASTER, 
			NO_NODES_FOUND
		};

		struct DecisionStruct {
			DecisionResult result;
			NodeId preferredPartner;
			u32 establishResult;
		};


		static constexpr int SIZEOF_NODE_MODULE_RESET_MESSAGE = 1;
		typedef struct
		{
			u8 resetSeconds;

		} NodeModuleResetMessage;
		STATIC_ASSERT_SIZE(NodeModuleResetMessage, 1);

#pragma pack(push)
#pragma pack(1)
		typedef struct
		{
			NodeId preferredPartnerIds[Conf::MAX_AMOUNT_PREFERRED_PARTNER_IDS];
			u8 amountOfPreferredPartnerIds;
			PreferredConnectionMode preferredConnectionMode;
		} PreferredConnectionMessage;
		STATIC_ASSERT_SIZE(PreferredConnectionMessage, 18);
#pragma pack(pop)

		//Node
		Node();
		void Init();
		bool IsInit();
		void ConfigurationLoadedHandler(ModuleConfiguration* migratableConfig, u16 migratableConfigLength) override;
		void ResetToDefaultConfiguration() override;

		void InitializeMeshGattService();

		//Connection
		void HandshakeTimeoutHandler() const;
		void HandshakeDoneHandler(MeshConnection* connection, bool completedAsWinner); 
		MeshAccessAuthorization CheckMeshAccessPacketAuthorization(BaseConnectionSendData* sendData, u8* data, u32 fmKeyId, DataDirection direction) override;

		void SendComponentMessage(connPacketComponentMessage& message, u16 payloadSize);

		//Stuff
		Node::DecisionStruct DetermineBestClusterAvailable(void);
		void UpdateJoinMePacket() const;

		//States
		void ChangeState(DiscoveryState newState);
		void DisableStateMachine(bool disable); //Disables the ChangeState function and does therefore kill all automatic mesh functionality

		void KeepHighDiscoveryActive();

		//Connection handlers
		//Message handlers
		void GapAdvertisementMessageHandler(const GapAdvertisementReportEvent& advertisementReportEvent);
		joinMeBufferPacket* findTargetBuffer(const advPacketJoinMeV0* packet);

		//Timers
		void TimerEventHandler(u16 passedTimeDs) override;

		//Helpers
		ClusterId GenerateClusterID(void) const;

		Module* GetModuleById(ModuleId id) const;

		void SendClusterInfoUpdate(MeshConnection* ignoreConnection, connPacketClusterInfoUpdate* packet) const;
		void ReceiveClusterInfoUpdate(MeshConnection* connection, connPacketClusterInfoUpdate* packet);

		void HandOverMasterBitIfNecessary() const;
		
		bool HasAllMasterBits() const;

		u32 CalculateClusterScoreAsMaster(joinMeBufferPacket* packet) const;
		u32 CalculateClusterScoreAsSlave(joinMeBufferPacket* packet) const;
		void PrintStatus() const;
		void PrintBufferStatus() const;
		void SetTerminalTitle() const;
#if defined(NRF52) || defined(SIM_ENABLED)
		CapabilityEntry GetCapability(u32 index) override;
		CapabilityEntry GetNextGlobalCapability();
#endif

		void StartConnectionRSSIMeasurement(MeshConnection& connection);
		void StopConnectionRSSIMeasurement(const MeshConnection& connection);

		void Reboot(u32 delayDs, RebootReason reason);
		bool IsRebootScheduled();

		//Receiving
		void MeshMessageReceivedHandler(BaseConnection* connection, BaseConnectionSendData* sendData, connPacketHeader* packetHeader) override;

		//Methods of TerminalCommandListener
		#ifdef TERMINAL_ENABLED
		bool TerminalCommandHandler(char* commandArgs[], u8 commandArgsSize) override;
		#endif

		//Methods of ConnectionManagerCallback
		void MeshConnectionDisconnectedHandler(AppDisconnectReason appDisconnectReason, ConnectionState connectionStateBeforeDisconnection, u8 hadConnectionMasterBit, i16 connectedClusterSize, u32 connectedClusterId);
		
		bool GetKey(u32 fmKeyId, u8* keyOut) const;
		bool IsPreferredConnection(NodeId id) const;

#if IS_ACTIVE(FAKE_NODE_POSITIONS)
		bool modifyEventForFakePositions(GapAdvertisementReportEvent& advertisementReportEvent) const;
#endif
};
