package de.hdm.smart_penguins.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.utils.PermissionDependentTask

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        startBLEScanning();
    }

    override fun onPause() {
        super.onPause()
        stopBLEScanning()
    }

    private fun startBLEScanning() {
        executeTaskOnPermissionGranted(
            object : PermissionDependentTask {
                override fun getRequiredPermission() =
                    android.Manifest.permission.ACCESS_FINE_LOCATION

                override fun onPermissionGranted() {
                    checkForBlePermission()
                }

                @SuppressLint("WrongConstant")
                override fun onPermissionRevoked() {
                    Toast
                        .makeText(
                            this@MainActivity,
                            "Cannot scan without Permissions",
                            Toast.LENGTH_LONG
                        )
                        .show();
                }
            })
    }

    private fun checkForBlePermission() {
        executeTaskOnPermissionGranted(
            object : PermissionDependentTask {
                override fun getRequiredPermission() =
                    android.Manifest.permission.BLUETOOTH

                override fun onPermissionGranted() {
                    initBLEScanning()
                }

                @SuppressLint("WrongConstant")
                override fun onPermissionRevoked() {
                    Toast
                        .makeText(
                            this@MainActivity,
                            "Cannot scan without Permissions",
                            Toast.LENGTH_LONG
                        )
                        .show();
                }
            })
    }
}
