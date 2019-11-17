package de.hdm.smart_penguins.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)   // Programmatically initialize the scanner view
        setContentView(mScannerView)                // Set the scanner view as the content view
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView!!.startCamera()          // Start camera on resume
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