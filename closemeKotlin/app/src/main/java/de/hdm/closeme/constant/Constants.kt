package de.hdm.closeme.constant

class Constants {
    companion object {

        //Place Types
        val PLACE_TYPE_DOOR_BEACON = 0
        val PLACE_TYPE_WINDOW_BEACON = 1
        val PLACE_TYPE_FLOOR_BEACON = 2
        val PLACE_TYPE_NOT_PLACED = -1

        //Board Types
        val RUUVI_TAG_STRING = "Ruuvi Tag"
        val RUUVI_TAG: Short = 3
        val DEV_BOARD_STRING = "Dev Board"
        val DEV_BOARD: Short = 1
        val ARCONNA_STRING = "Arconna Board"
        val ARCONNA: Short = 2
        val UNKOWN_BOARD = "Unkown Board Type"

        //Message Types
        val BLE_DATA_TYPE_ALARM_MESSAGE = 33
        val BLE_DATA_TYPE_MESH_MESSAGE = 4
        val MESSAGE_TYPE_ALARM_UPDATE = 25
        val DATA_TYPE_WINDOW_VERSION_ONE : Short = 78


        //Argument
        val ARGUMENT = "argument"
        val ARGUMENT_ALERT_MODE = "alert_mode"
        val ARGUMENT_SERVICE_STATE = "service_state"
        val ARGUMENT_SETUP_MODE = "setup_mode"
        val ARGUMENT_SETUP_FIRST_PAGE = "setup_first_page"
        val ARGUMENT_SETUP_SECOND_PAGE = "setup_second_page"
        val ACTION_STARTFOREGROUND = "com.hdm.closeme.service.clickAction.startforeground"
        val ACTION_STOPFOREGROUND = "com.hdm.closeme.service.clickAction.stopforeground"
        val ARGUMENT_SPOT_COUNT = "argument_spot_count"


        val NOTIFICATION_ID_FOREGROUND_SERVICE = 101
        val EMPTY_STRING: String = ""
        val PERMISSION_REQUEST_CODE = 3526
        val EXPIRY_TIME = 60000
        val EXPIRY_HANDLER_TIME: Long = 10000
        val MESH_DISCONNECTION_TIME: Long = 3600000


        //LOG TAGS
        val TAG_BLUETOOTH_SERVICE = "bluetooth_service"
        val TAG_EMPTY = ""
        val TAG_SETUP_FRAGMENT = "setup_fragment"
        val TAG_SCANNER_FRAGMENT = "scanner_fragment"
        val TAG_MAP_FRAGMENT = "map_fragment"
        val TAG_HOME_FRAGMENT = "home_fragment"
        val TAG_MAIN_ACTIVITY = "main_activity"


        val NAVIGATION_ITEM_SCANNER = 0
        val NAVIGATION_ITEM_HOME = 1
        val NAVIGATION_ITEM_MAP = 2
        val NAVIGATION_ITEM_BACK = 3
        val NAVIGATION_ITEM_FORWARD = 4

        //Empty values
        val NO_VALUE_CLUSTER_ID: Short = -1
        val NO_VALUE_RSSI = 0
        val NO_VALUE_HUMIDITY: Short = -255
        val NO_VALUE_TEMPERATURE: Short = 0xFF
        val NO_VALUE_DISTANCE = -1.0f
        val NO_VALUE_CHANNEL: Short = -1
        val NO_SENSOR_TEMPERATURE: Short = -128
        val NO_SENSOR_HUMIDITY: Short = 255
        val NO_VALUE_SPOT_NUMBER: Short = 0
        val NO_VALUE_LATLNG = -1.0


        //PARAMs
        val PARAM_VIBRATION_TIME: Long = 150
        val PARAM_ZOOM_LEVEL = 19.5
        val PARAM_INTENT_TIME = 60000
        val PARAM_SCAN_RECORD_OFFSET = 7

        val ACTIVE_STATE_ACTIVE = 1
        val PARAM_IN_RANGE_DISTANCE: Float = 100F
        val PARAM_IN_RANGE_DOOR: Float = 1F
        val ALERT_TEMPERATURE: Short = 20
        val SETTINGS_PATH = "com.android.settings"
        val SETTINGS_PATH_BLUETOOTH = "com.android.settings.bluetooth.BluetoothSettings"
        val PARAM_COORD_MWAY_LAT_SETUP = 48.808304
        val PARAM_COORD_MWAY_LNG_SETUP = 9.178907
        val PARAM_COORD_MWAY_LAT = 48.808666
        val PARAM_COORD_MWAY_LNG = 9.178941
        val TAG_WAKE_UP = "closeme:wakeup"
        val PARAM_MAP_ICON_SIZE = 80
    }

}