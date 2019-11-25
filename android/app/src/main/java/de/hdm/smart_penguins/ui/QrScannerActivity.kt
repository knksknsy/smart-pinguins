package de.hdm.smart_penguins.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import de.hdm.smart_penguins.utils.PermissionDependentTask
import de.hdm.smart_penguins.utils.PermissionsHandler.executeTaskOnPermissionGranted
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScannerActivity : BaseActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this)
        enableCamera()
    }

    private fun enableCamera() {
        executeTaskOnPermissionGranted(
            object : PermissionDependentTask {
                override fun getRequiredPermission() =
                    android.Manifest.permission.CAMERA

                override fun onPermissionGranted() {
                    mScannerView!!.startCamera()
                }

                @SuppressLint("WrongConstant")
                override fun onPermissionRevoked() {
                    Toast
                        .makeText(
                            this@QrScannerActivity,
                            "Cannot scan without Permissions",
                            Toast.LENGTH_LONG
                        )
                        .show();
                }
            })
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()           // Stop camera on pause
    }

    override fun handleResult(rawResult: com.google.zxing.Result?) {

        Log.e("RESULT", rawResult.toString())
        onBackPressed()

    }

}