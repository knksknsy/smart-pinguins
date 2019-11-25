package de.hdm.smart_penguins.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun isPermissionGranted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

fun isPermissionDenied(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED

fun requestPermission(activity: AppCompatActivity, permission: String, responseId: Int) {
    ActivityCompat.requestPermissions(activity, arrayOf(permission), responseId)
}

interface PermissionDependentTask {
    fun getRequiredPermission(): String
    fun onPermissionGranted()
    fun onPermissionRevoked()
}

object PermissionsHandler {

    private const val PERMISSION_ID = 1234

    private var pendingTask: PermissionDependentTask? = null

    fun executeTaskOnPermissionGranted(context: AppCompatActivity, task: PermissionDependentTask) {
        if (isPermissionDenied(context,
                task.getRequiredPermission())) {
            requestPermission(context,
                task.getRequiredPermission(),
                PERMISSION_ID)
            if (pendingTask == null) pendingTask = task
        } else {
            task.onPermissionGranted()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>,
                                   grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_ID -> {
                when (PermissionResult.convert(requestCode, permissions, grantResults)) {
                    is PermissionsHandler.PermissionResult.Granted -> {
                        if (pendingTask != null) {
                            pendingTask?.onPermissionGranted()
                            pendingTask = null
                        }
                    }
                    is PermissionsHandler.PermissionResult.Revoked -> {
                        pendingTask?.onPermissionRevoked()
                    }
                    is PermissionsHandler.PermissionResult.Cancelled -> {

                    }
                }
            }
            else -> {
            }
        }
    }

    sealed class PermissionResult {
        class Granted(id: Int) : PermissionResult()
        class Revoked(id: Int) : PermissionResult()
        class Cancelled(id: Int) : PermissionResult()

        companion object {
            fun convert(requestCode: Int,
                        permissions: Array<String>,
                        grantResults: IntArray): PermissionResult {
                return when {
                    grantResults.isEmpty() -> Cancelled(requestCode)
                    grantResults.first() == PackageManager.PERMISSION_GRANTED -> Granted(requestCode)
                    else -> Revoked(requestCode)
                }
            }
        }
    }


}