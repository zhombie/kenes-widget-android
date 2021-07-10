package q19.kenes.widget.core.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.webkit.WebSettings
import androidx.core.content.ContextCompat
import java.io.File
import java.util.*

internal class DeviceInfo constructor(private val context: Context) {

    companion object {
        private const val PLATFORM = "android"

        private const val BATTERY_HEALTH_COLD = "cold"
        private const val BATTERY_HEALTH_DEAD = "dead"
        private const val BATTERY_HEALTH_GOOD = "good"
        private const val BATTERY_HEALTH_OVERHEAT = "Over Heat"
        private const val BATTERY_HEALTH_OVER_VOLTAGE = "Over Voltage"
        private const val BATTERY_HEALTH_UNKNOWN = "Unknown"
        private const val BATTERY_HEALTH_UNSPECIFIED_FAILURE = "Unspecified failure"

        private const val BATTERY_PLUGGED_AC = "Charging via AC"
        private const val BATTERY_PLUGGED_USB = "Charging via USB"
        private const val BATTERY_PLUGGED_WIRELESS = "Wireless"
        private const val BATTERY_PLUGGED_UNKNOWN = "Unknown Source"

        private const val RINGER_MODE_NORMAL = "Normal"
        private const val RINGER_MODE_SILENT = "Silent"
        private const val RINGER_MODE_VIBRATE = "Vibrate"

        private const val PHONE_TYPE_GSM = "GSM"
        private const val PHONE_TYPE_CDMA = "CDMA"
        private const val PHONE_TYPE_NONE = "Unknown"

        private const val NETWORK_TYPE_2G = "2G"
        private const val NETWORK_TYPE_3G = "3G"
        private const val NETWORK_TYPE_4G = "4G"
        private const val NETWORK_TYPE_WIFI_WIFIMAX = "WiFi"

        private const val NOT_FOUND_VAL = "unknown"
    }

    /* Device Info: */
    val os: String
        get() = PLATFORM

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model
            } else {
                "$manufacturer $model"
            }
        }

    val releaseBuildVersion: String
        get() = Build.VERSION.RELEASE

    val buildVersionCodeName: String
        get() = Build.VERSION.CODENAME

    val manufacturer: String
        get() = Build.MANUFACTURER

    val model: String
        get() = Build.MODEL


    val product: String
        get() = Build.PRODUCT

    val fingerprint: String
        get() = Build.FINGERPRINT

    val hardware: String
        get() = Build.HARDWARE


    val radioVer: String
        get() = Build.getRadioVersion()


    val device: String
        get() = Build.DEVICE

    val board: String
        get() = Build.BOARD

    val displayVersion: String
        get() = Build.DISPLAY

    val buildBrand: String
        get() = Build.BRAND

    val buildHost: String
        get() = Build.HOST

    val buildTime: Long
        get() = Build.TIME

    val buildUser: String
        get() = Build.USER

    val osVersion: String
        get() = Build.VERSION.RELEASE

    val language: String
        get() = Locale.getDefault().language

    val sdkVersion: Int
        get() = Build.VERSION.SDK_INT

    val screenDensity: String
        get() {
            return when (context.resources.displayMetrics.densityDpi) {
                DisplayMetrics.DENSITY_LOW -> "ldpi"
                DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
                DisplayMetrics.DENSITY_HIGH -> "hdpi"
                DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
                else -> "other"
            }
        }


    /* App Info: */
    val versionName: String?
        get() {
            val pInfo: PackageInfo
            return try {
                pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName
            } catch (e1: Exception) {
                null
            }
        }

    val versionCode: Long?
        get() {
            val packageInfo: PackageInfo
            return try {
                packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } catch (e1: Exception) {
                null
            }
        }

    val packageName: String
        get() = context.packageName

    val activityName: String
        get() = context.javaClass.simpleName

    val appName: String
        get() {
            val packageManager = context.packageManager
            var applicationInfo: ApplicationInfo? = null
            try {
                applicationInfo =
                    packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
            }

            return if (applicationInfo != null) {
                packageManager.getApplicationLabel(applicationInfo).toString()
            } else {
                NOT_FOUND_VAL
            }
        }


    private val batteryStatusIntent: Intent?
        get() {
            val batFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            return context.registerReceiver(null, batFilter)
        }

    val batteryPercent: Double
        get() {
            val intent = batteryStatusIntent
            val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            var level: Double = -1.0
            if (rawLevel >= 0 && scale > 0) {
                level = rawLevel * 100.0 / scale
            }
            return level
        }

    val isPhoneCharging: Boolean
        get() {
            val intent = batteryStatusIntent
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
        }

    val batteryHealth: String
        get() {
            val intent = batteryStatusIntent
            return when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_COLD -> BATTERY_HEALTH_COLD
                BatteryManager.BATTERY_HEALTH_DEAD -> BATTERY_HEALTH_DEAD
                BatteryManager.BATTERY_HEALTH_GOOD -> BATTERY_HEALTH_GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BATTERY_HEALTH_OVERHEAT
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BATTERY_HEALTH_OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> BATTERY_HEALTH_UNKNOWN
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BATTERY_HEALTH_UNSPECIFIED_FAILURE
                else -> BATTERY_HEALTH_UNKNOWN
            }
        }

    val batteryTechnology: String
        get() {
            val intent = batteryStatusIntent
            return intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
        }

    val batteryTemperature: Float
        get() {
            val intent = batteryStatusIntent
            val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            return temperature / 10.0F
        }

    val batteryVoltage: Int
        get() {
            val intent = batteryStatusIntent
            return intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        }

    val chargingSource: String
        get() {
            val intent = batteryStatusIntent
            return when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> BATTERY_PLUGGED_AC
                BatteryManager.BATTERY_PLUGGED_USB -> BATTERY_PLUGGED_USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BATTERY_PLUGGED_WIRELESS
                else -> BATTERY_PLUGGED_UNKNOWN
            }
        }

    val isBatteryPresent: Boolean
        get() {
            val intent = batteryStatusIntent
            return intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) ?: false
        }


    val isRunningOnEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("vbox86p")
                || Build.DEVICE.contains("vbox86p")
                || Build.HARDWARE.contains("vbox86"))

    val deviceRingerMode: String
        get() {
            val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)
            return when (audioManager?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> RINGER_MODE_SILENT
                AudioManager.RINGER_MODE_VIBRATE -> RINGER_MODE_VIBRATE
                else -> RINGER_MODE_NORMAL
            }
        }

    val isDeviceRooted: Boolean
        get() {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            return false
        }


    val userAgent: String
        get() {
            val systemUa = System.getProperty("http.agent")
            return WebSettings.getDefaultUserAgent(context) + "__" + systemUa
        }


    val totalRAM: Long
        get() {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = ContextCompat.getSystemService(context, ActivityManager::class.java)
            activityManager?.getMemoryInfo(memoryInfo)
            return memoryInfo.totalMem
        }

    val availableInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize: Long
            val availableBlocks: Long
            blockSize = stat.blockSizeLong
            availableBlocks = stat.availableBlocksLong
            return availableBlocks * blockSize
        }

    val totalInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize: Long
            val totalBlocks: Long
            blockSize = stat.blockSizeLong
            totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        }


    val phoneType: String
        get() {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return when (telephonyManager?.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> PHONE_TYPE_GSM
                TelephonyManager.PHONE_TYPE_CDMA -> PHONE_TYPE_CDMA
                TelephonyManager.PHONE_TYPE_NONE -> PHONE_TYPE_NONE
                else -> PHONE_TYPE_NONE
            }
        }


    val operator: String?
        get() {
            var operatorName: String?
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            operatorName = telephonyManager?.networkOperatorName
            if (operatorName == null) {
                operatorName = telephonyManager?.simOperatorName
            }
            return operatorName
        }


    val isSimNetworkLocked: Boolean
        get() {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return telephonyManager?.simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
        }


    val isNfcEnabled: Boolean
        get() {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            return nfcAdapter != null && nfcAdapter.isEnabled
        }

    val isWifiEnabled: Boolean
        get() {
            val wifiManager: WifiManager? = ContextCompat.getSystemService(context, WifiManager::class.java)
            return wifiManager?.isWifiEnabled == true
        }

    fun isAppInstalled(packageName: String): Boolean {
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    fun hasExternalSDCard(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

}