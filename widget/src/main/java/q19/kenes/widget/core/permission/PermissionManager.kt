package q19.kenes.widget.core.permission

import android.Manifest
import androidx.fragment.app.FragmentActivity
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.anyPermanentlyDenied
import com.fondesa.kpermissions.anyShouldShowRationale
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.fondesa.kpermissions.request.PermissionRequest
import q19.kenes_widget.R
import q19.kenes.widget.util.createAppSettingsIntent
import q19.kenes.widget.util.showPermanentlyDeniedDialog

internal class PermissionManager constructor(private val fragmentActivity: FragmentActivity) {

    private val externalStoragePermissionRequest by lazy {
        fragmentActivity.permissionsBuilder(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).build()
    }

    private val audioCallPermissionRequest by lazy {
        fragmentActivity.permissionsBuilder(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH
        ).build()
    }

    private val videoCallPermissionRequest by lazy {
        fragmentActivity.permissionsBuilder(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH
        ).build()
    }

    enum class Permission {
        EXTERNAL_STORAGE,
        AUDIO_CALL,
        VIDEO_CALL
    }

    fun checkPermission(
        permission: Permission,
        callback: (isPositive: Boolean) -> Unit
    ) {
        val permissionRequest: PermissionRequest
        val formattedPermissions: String

        when (permission) {
            Permission.EXTERNAL_STORAGE -> {
                permissionRequest = externalStoragePermissionRequest
                formattedPermissions = formatEachWithIndex(
                    fragmentActivity.getString(R.string.kenes_storage)
                )
            }
            Permission.AUDIO_CALL -> {
                permissionRequest = audioCallPermissionRequest
                formattedPermissions = formatEachWithIndex(
                    fragmentActivity.getString(R.string.kenes_microphone),
                    fragmentActivity.getString(R.string.kenes_storage)
                )
            }
            Permission.VIDEO_CALL -> {
                permissionRequest = videoCallPermissionRequest
                formattedPermissions = formatEachWithIndex(
                    fragmentActivity.getString(R.string.kenes_camera),
                    fragmentActivity.getString(R.string.kenes_microphone),
                    fragmentActivity.getString(R.string.kenes_storage)
                )
            }
        }

        checkPermissions(
            permissionRequest,
            fragmentActivity.getString(
                R.string.kenes_permissions_necessity_info,
                formattedPermissions
            )
        ) {
            callback(it)
        }
    }

    private fun formatEachWithIndex(vararg args: String): String {
        return args
            .mapIndexed { index, s -> "${(index + 1)}. $s" }
            .joinToString(separator = "\n")
    }

    private fun checkPermissions(
        permissionRequest: PermissionRequest,
        alertText: String,
        callback: (isPositive: Boolean) -> Unit
    ) {
        val permissionStatuses = permissionRequest.checkStatus()

        if (permissionStatuses.allGranted()) {
            callback(true)
        } else {
            permissionRequest.send { result ->
                if (result.allGranted()) {
                    callback(true)
                    return@send
                }

                val isAnyPermanentlyDenied = result.anyPermanentlyDenied()
                val isAnyShouldShowRationale = result.anyShouldShowRationale()

                val positiveButtonText = if (isAnyPermanentlyDenied) {
                    fragmentActivity.getString(R.string.kenes_to_settings)
                } else {
                    fragmentActivity.getString(R.string.kenes_ok)
                }

                fragmentActivity.showPermanentlyDeniedDialog(
                    alertText,
                    positiveButtonText
                ) { isPositive ->
                    if (isPositive) {
                        if (isAnyPermanentlyDenied) {
                            fragmentActivity.startActivity(fragmentActivity.createAppSettingsIntent())
                        } else if (isAnyShouldShowRationale) {
                            permissionRequest.send()
                        }
                    } else {
                        callback(false)
                    }
                }
            }
        }
    }

    fun removeAllListeners() {
        externalStoragePermissionRequest.removeAllListeners()
        audioCallPermissionRequest.removeAllListeners()
        videoCallPermissionRequest.removeAllListeners()
    }

}