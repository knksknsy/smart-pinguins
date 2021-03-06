package de.hdm.smart_penguins.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import de.hdm.smart_penguins.data.model.PersistentNodeList
import de.hdm.smart_penguins.utils.PermissionDependentTask
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScannerActivity : BaseActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null
    private var TAG = "QR_SCANNER"

    companion object {
        val RAW_RESULT = "raw_result"
    }

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
        if (rawResult != null) {
            try {
                val nodeList = PersistentNodeList.fromJson(rawResult.toString())
                if (nodeList != null && nodeList.value.stream()
                        .noneMatch { x ->
                            dataManager.qrScannedNodes.stream()
                                .anyMatch { b -> b.nodeID == x.nodeID }
                        }
                ) {
                    dataManager.qrScannedNodes.addAll(nodeList.value)
                }
            } catch (exception: Exception) {
                Log.e(TAG, exception.toString())
            }
            finish()
            Log.e("RESULT", rawResult.toString())
        }
        onBackPressed()

    }

}