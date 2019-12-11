package de.hdm.smart_penguins.ui.map


import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.manager.DataManager
import de.hdm.smart_penguins.ui.QrScannerActivity
import kotlinx.android.synthetic.main.bottomsheet_layout.*
import kotlinx.android.synthetic.main.fragment_map.*
import javax.inject.Inject

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    @Inject
    lateinit var dataManager: DataManager

    private var gMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var setupMode = false
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var positionMarker: Marker? = null
    private var isLocationEnabled = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        getMyApplication().activityComponent?.inject(this)
        return inflater.inflate(R.layout.fragment_map, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        upButton.setOnClickListener { expandBottomSheet() }
        infoText.setOnClickListener { expandBottomSheet() }
        mapFragment = mapFragment
            ?: childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        fabPlus.setOnClickListener {
            val intent = Intent(context, QrScannerActivity::class.java)
            startActivity(intent);
        }
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
        if (dataManager.persistenNode != null) {
            infoText.text = "Add node: " + dataManager.persistenNode
        }
    }


    private fun setMarker(placedBeaconType: Int, point: LatLng) {

    }


    fun createMarkerOptions(): MarkerOptions {

        val markerOptions = MarkerOptions().position(LatLng(1.2, 2.2))
        val bitmap = vectorToBitmap(
            R.drawable.common_google_signin_btn_icon_dark
        )
        markerOptions.icon(bitmap)
        return markerOptions
    }

    private fun vectorToBitmap(@DrawableRes id: Int): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(resources, id, null)
        val bitmap = resizeMapIcons(
            Bitmap.createBitmap(
                vectorDrawable!!.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun setPosition() {

        val markerOptions = MarkerOptions().position(LatLng(1.1, 1.1))
        val bitmap = vectorToBitmap(R.drawable.ic_home_black_24dp)

        markerOptions.icon(bitmap)
        markerOptions.zIndex(1f)
        positionMarker?.remove()
        positionMarker = gMap!!.addMarker(markerOptions)
        setAnimation(positionMarker)
        gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(positionMarker!!.position, 20f))
        positionMarker?.showInfoWindow()
        positionMarker?.title = "YOU ARE HERE"
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


    private fun isZoomedIn(): Boolean {
        if (gMap!!.cameraPosition != null) {
            return gMap!!.cameraPosition.zoom >= Constants.PARAM_ZOOM_LEVEL
        } else return false
    }


    fun resizeMapIcons(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(
            bitmap,
            Constants.PARAM_MAP_ICON_SIZE,
            Constants.PARAM_MAP_ICON_SIZE,
            false
        )
    }


    override fun onMapReady(googleMap: GoogleMap) {
        if (gMap == null) {
            gMap = googleMap
            gMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))
            gMap!!.isBuildingsEnabled = false
            // zoom to our floor
            val mway = if (setupMode) LatLng(
                Constants.PARAM_COORD_MWAY_LAT_SETUP,
                Constants.PARAM_COORD_MWAY_LNG_SETUP
            ) else LatLng(Constants.PARAM_COORD_MWAY_LAT, Constants.PARAM_COORD_MWAY_LNG)
            gMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(mway, 19f))
            gMap!!.setOnCameraMoveListener(this@MapFragment)
        }
    }

    fun setGroundOverlay() {
        //Center the ground plan
        val mway = LatLng(48.808657, 9.178871)
        val mwayMap = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.common_full_open_on_phone))
            .position(mway, 130f)
        gMap!!.addGroundOverlay(mwayMap)
    }

    override fun onPause() {
        super.onPause()
        mapFragment?.onPause()
        positionMarker == null
    }

    override fun onCameraMove() {
    }

    fun getMyApplication(): SmartApplication =
        this.requireActivity().application as SmartApplication
}
