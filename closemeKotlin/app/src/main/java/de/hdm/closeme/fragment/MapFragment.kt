package de.hdm.closeme.fragment


import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import de.hdm.closeme.MainActivity
import de.hdm.closeme.R
import de.hdm.closeme.adapter.MapListAdapter
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.listener.BeaconListListener
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconList
import kotlinx.android.synthetic.main.bottomsheet_layout.*
import kotlinx.android.synthetic.main.fragment_map.*


/**
 * A simple [Fragment] subclass.
 *
 */
class MapsFragment : Fragment(), OnMapReadyCallback, BeaconListListener, GoogleMap.OnCameraMoveListener {


    private var beaconArrayList = AlarmBeaconList()
    private var adapter: MapListAdapter? = null
    private var gMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var setupMode = false
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var alertBeaconDeviceNumber: String? = null
    private var markerVisible = false
    private var positionMarker: Marker? = null
    private var isLocationEnabled = false

    companion object {

        @JvmStatic
        fun newInstance(setupMode: Boolean, beaconDeviceNumber: String?) = MapsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(Constants.ARGUMENT_SETUP_MODE, setupMode)
                putString(Constants.ARGUMENT_ALERT_MODE, beaconDeviceNumber)
            }
        }
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        initView(isVisibleToUser)

    }

    private fun initView(visibleToUser: Boolean) {
        if (visibleToUser) {
            registerBeaconUpdateListener()
            setInfoText()
            initiateMap()
        } else {
            if (this.view != null) unregisterBeaconUpdateListener()
        }
    }


    private fun setInfoText() {
        infoText.text =
                if (!setupMode)
                    getString(R.string.information_list_one)
                else if (this.beaconArrayList.size > 0)
                    getString(R.string.information_list_two)
                else
                    getString(R.string.information_list_three)

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        arguments?.getBoolean(Constants.ARGUMENT_SETUP_MODE)?.let { this.setupMode = it }
        arguments?.getString(Constants.ARGUMENT_ALERT_MODE)?.let { this.alertBeaconDeviceNumber = it }
        this.alertBeaconDeviceNumber = "20"
        return inflater.inflate(R.layout.fragment_map, container, false)
    }


    override fun onBeaconListChanged(list: AlarmBeaconList) {
        this.beaconArrayList = ((activity) as MainActivity).getBeaconList()
        this.beaconArrayList.sortByRange()
        if (setupMode && gMap != null) removeUnusedMarker()
        adapter?.updateBeaconList(
                if (setupMode) beaconArrayList.removeUnusedBeacons()
                else beaconArrayList)
        adapter?.notifyDataSetChanged()
        if (this.beaconArrayList.size > 0) {
            if (positionMarker == null || beaconArrayList[0].marker?.position?.latitude != positionMarker!!.position?.latitude) {
                setPosition()
                Log.e(Constants.TAG_MAP_FRAGMENT, "Position Update")
            }
        }
    }

    private fun removeUnusedMarker() {
        for (beacon in beaconArrayList) {
            if (!beacon.isUsed) {
                if(beacon.marker != null) beacon.marker!!.remove()
            }
        }
    }

    private fun updateMap() {
        for (beacon in beaconArrayList) {
            if (beacon.placedType == Constants.PLACE_TYPE_WINDOW_BEACON && beacon.marker != null) {
                setMapPoint(beacon)
            }
        }
        setPosition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        upButton.setOnClickListener { expandBottomSheet() }
        infoText.setOnClickListener { expandBottomSheet() }
        mapFragment = mapFragment
                ?: childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        if (!setupMode) {
            bottomSheet.visibility = View.GONE
            fabLocalization.setOnClickListener {
                isLocationEnabled = !isLocationEnabled
                fabLocalization.setColorFilter(if (isLocationEnabled) resources.getColor(R.color.CornflowerBlue, null) else resources.getColor(R.color.Black, null))
                setPosition()
            }
            fabHome.setOnClickListener {
                if (gMap != null) gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(Constants.PARAM_COORD_MWAY_LAT, Constants.PARAM_COORD_MWAY_LNG), 19f))
            }
        } else {
            fabLocalization.hide()
            fabHome.hide()

        }
        showCurrentlyPlacingText(null)

    }

    private fun expandBottomSheet() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED)
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        else
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

    }

    override fun onResume() {
        super.onResume()
        mapFragment?.getMapAsync(this)
        setBeaconList()
        if (!setupMode || userVisibleHint) {
            registerBeaconUpdateListener()
            onBeaconListChanged(((activity) as MainActivity).getBeaconList())
        }

    }

    private fun registerBeaconUpdateListener() {
        ((activity) as MainActivity).registerBeaconListListener(this)
    }

    private fun unregisterBeaconUpdateListener() {
        ((activity) as MainActivity).unregisterBeaconListListener()
    }


    private fun setBeaconList() {
        mapBeaconList.layoutManager = LinearLayoutManager(context)
        this.beaconArrayList = ((activity) as MainActivity).getBeaconList()
        this.beaconArrayList.sortByRange()
        if (setupMode) {
            adapter = adapter
                    ?: MapListAdapter(beaconArrayList.removeUnusedBeacons(), context, { _, _ ->
                        //((activity) as MainActivity).onBeaconChanged(beacon, position)
                    }, { beacon ->
                        placeBeaconAtMap(beacon)
                        onBeaconListChanged(((activity) as MainActivity).getBeaconList())
                    })
            mapBeaconList.adapter = adapter
        }
    }

    private fun placeBeaconAtMap(beacon: AlarmBeacon) {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        showCurrentlyPlacingText(beacon)
        gMap?.setOnMapClickListener { point ->
            showBeaconPlacedDialog(point, beacon)
        }

    }


    private fun showBeaconPlacedDialog(point: LatLng, beacon: AlarmBeacon) {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.dialog_placing_title))
        builder.setMessage(getString(R.string.dialog_placing_question))
        builder.setPositiveButton(getString(R.string.dialog_placing_window)) { _, _ ->
            setMarker(Constants.PLACE_TYPE_WINDOW_BEACON, point, beacon)
        }
        builder.setNegativeButton(getString(R.string.dialog_placing_door)) { _, _ ->
            setMarker(Constants.PLACE_TYPE_DOOR_BEACON, point, beacon)
        }
        builder.setNeutralButton(getString(R.string.dialog_placing_floor)) { _, _ ->
            setMarker(Constants.PLACE_TYPE_FLOOR_BEACON, point, beacon)
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun setMarker(placedBeaconType: Int, point: LatLng, beacon: AlarmBeacon) {
        beacon.lat = point.latitude
        beacon.lng = point.longitude
        beacon.placedType = placedBeaconType
        //initiateMap()
        if (beacon.lat != Constants.NO_VALUE_LATLNG && beacon.lng != Constants.NO_VALUE_LATLNG) {
            setMapPoint(beacon)
        }
        showCurrentlyPlacingText(null)
        gMap?.setOnMapClickListener {}
        ((activity) as MainActivity).saveBeaconPersistent(beacon, true)
    }


    private fun showCurrentlyPlacingText(beacon: AlarmBeacon?) {

        currentPlacingLayout.visibility =
                if (beacon != null || alertBeaconDeviceNumber != null ) View.VISIBLE else View.GONE
        if(alertBeaconDeviceNumber != null){
            currentBeacon.text = getString(R.string.device_number, 20)
            currentlyPlacingTxt.text = getString(R.string.alert)
            currentlyPlacingTxt.setTextColor(resources.getColor(R.color.colorPrimary,null))
            currentBeacon.setTextColor(resources.getColor(R.color.colorPrimary,null))
        }else{
            currentBeacon.text = getString(R.string.device_number, beacon?.deviceNumber)
        }
    }


    private fun setMapPoint(beacon: AlarmBeacon) {
        if (gMap != null) {
            beacon.marker?.remove()
            //Marker Options
            val isAlertedWindow = alertBeaconDeviceNumber != null
                    && TextUtils.equals(alertBeaconDeviceNumber,beacon.deviceNumber.toString())
            beacon.marker = gMap!!.addMarker(createMarkerOptions(beacon))
            if (beacon.marker != null) beacon.marker!!.title = beacon.deviceNumber.toString()

            //Enable fading for open windows and door point
            if (isAlertedWindow && beacon.isOpenWindow()) {
                setAnimation(beacon.marker)
            }
        }
    }


    fun createMarkerOptions(beacon: AlarmBeacon): MarkerOptions {
        val isOpenWindow = !setupMode && beacon.isOpenWindow()

        val markerOptions = MarkerOptions().position(LatLng(beacon.lat, beacon.lng))
        val bitmap = vectorToBitmap(
                if (beacon.placedType == Constants.PLACE_TYPE_WINDOW_BEACON) {
                    if (isOpenWindow) R.drawable.marker_window_open_vektor else R.drawable.marker_window_closed_vektor
                } else if (beacon.placedType == Constants.PLACE_TYPE_DOOR_BEACON) {
                    //if (isAlertingDoor) "marker_person" else "marker_door"
                    R.drawable.marker_door_vektor
                } else R.drawable.marker_floor_vektor
        )
        markerOptions.icon(bitmap)
        if (!setupMode) {
            markerOptions.visible(isZoomedIn())
            markerVisible = isZoomedIn()
        }
        return markerOptions
    }

    private fun vectorToBitmap(@DrawableRes id: Int): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(resources, id, null)
        val bitmap = resizeMapIcons(Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888))
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun setPosition() {
        if (!setupMode &&
                beaconArrayList.size > 0 &&
                beaconArrayList[0].distance != Constants.NO_VALUE_DISTANCE &&
                gMap != null &&
                isLocationEnabled &&
                beaconArrayList[0].isPlaced()) {
            val markerOptions = MarkerOptions().position(LatLng(beaconArrayList[0].lat, beaconArrayList[0].lng))
            val bitmap = vectorToBitmap(R.drawable.marker_person_vektor)

            markerOptions.icon(bitmap)
            markerOptions.zIndex(1f)
            positionMarker?.remove()
            positionMarker = gMap!!.addMarker(markerOptions)
            setAnimation(positionMarker)
            gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(positionMarker!!.position, 20f))
            Log.e(Constants.TAG_MAP_FRAGMENT, "Position set")
            positionMarker?.showInfoWindow()
            positionMarker?.title = "YOU ARE HERE"
        } else {
            positionMarker?.remove()
        }
    }

    private fun setAnimation(marker: Marker?) {
        val ani = ValueAnimator.ofFloat(1f, 0f)
        ani.setDuration(800)
        ani.setRepeatMode(ValueAnimator.REVERSE)
        ani.setRepeatCount(ValueAnimator.INFINITE)
        ani.addUpdateListener { animation ->
            if (marker != null) {
                marker.alpha = animation.animatedValue as Float
            }
        }
        ani.start();
    }

    override fun onCameraMove() {
        if (!setupMode) {
            if (markerVisible && gMap!!.cameraPosition.zoom < Constants.PARAM_ZOOM_LEVEL) {
                showMarkers(false)
            } else if (!markerVisible && gMap!!.cameraPosition.zoom >= Constants.PARAM_ZOOM_LEVEL) {
                showMarkers(true)
            }
        }
    }

    private fun isZoomedIn(): Boolean {
        if (gMap!!.cameraPosition != null) {
            return gMap!!.cameraPosition.zoom >= Constants.PARAM_ZOOM_LEVEL
        } else return false
    }

    private fun showMarkers(visible: Boolean) {
        for (beacon in beaconArrayList) {
            if (beacon.marker != null) {
                beacon.marker!!.isVisible = visible
            }
            markerVisible = visible
        }
    }

    fun resizeMapIcons(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, Constants.PARAM_MAP_ICON_SIZE, Constants.PARAM_MAP_ICON_SIZE, false)
    }

    private fun initiateMap() {
        setGroundOverlay()
        for (beacon in beaconArrayList) {
            if (beacon.isPlaced()) {
                setMapPoint(beacon)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (gMap == null) {
            gMap = googleMap
            gMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_json))
            gMap!!.isBuildingsEnabled = false
            // zoom to our floor
            val mway = if (setupMode) LatLng(Constants.PARAM_COORD_MWAY_LAT_SETUP, Constants.PARAM_COORD_MWAY_LNG_SETUP) else LatLng(Constants.PARAM_COORD_MWAY_LAT, Constants.PARAM_COORD_MWAY_LNG)
            gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(mway, 19f))
            initiateMap()
            if (alertBeaconDeviceNumber != null) showAlert()
            gMap!!.setOnCameraMoveListener(this@MapsFragment)
        }
    }

    private fun showAlert() {
        val alertPosition: LatLng? = beaconArrayList.getBeaconForNumber(alertBeaconDeviceNumber!!.toShort())?.marker?.position
        gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(
                if (alertPosition != null) alertPosition else LatLng(Constants.PARAM_COORD_MWAY_LAT, Constants.PARAM_COORD_MWAY_LNG), 20f))
    }


    fun setGroundOverlay() {
        //Center the ground plan
        val mway = LatLng(48.808657, 9.178871)
        val mwayMap = GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.mway_map_cutted))
                .position(mway, 130f)
        gMap!!.addGroundOverlay(mwayMap)
    }

    override fun onPause() {
        super.onPause()
        mapFragment?.onPause()
        if (!setupMode) unregisterBeaconUpdateListener()
        positionMarker == null
    }

    override fun onNotificationStatusChanged() {
        updateMap()
    }
}
