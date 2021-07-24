package q19.kenes.widget.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.webrtc.IceServer

class Database private constructor(context: Context) {

    companion object {
        private val TAG = Database::class.java.simpleName

        @Volatile
        private var INSTANCE: Database? = null

        fun getInstance(context: Context): Database =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Database(context).also { INSTANCE = it }
            }
    }

    init {
        Log.d(TAG, "created")
    }

    private object Preferences {
        private const val DEFAULT_NAME = "kenes.widget.preferences"

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(DEFAULT_NAME, Context.MODE_PRIVATE)
        }

        object Key {
            const val CONFIGS = "configs"
        }
    }

    private val sharedPreferences by lazy { Preferences.getSharedPreferences(context) }

    private var configs: Configs? = null
    private var iceServers: List<IceServer>? = null

    private var onUpdateConfigsListener: OnUpdateConfigsListener? = null
    private var onUpdateIceServersListener: OnUpdateIceServersListener? = null

    fun setOnUpdateConfigsListener(listener: OnUpdateConfigsListener?) {
        if (listener == null) {
            onUpdateConfigsListener = listener
        } else {
            onUpdateConfigsListener = listener

            val configs = getConfigs()
            if (configs != null) {
                onUpdateConfigsListener?.onUpdate(configs)
            }
        }
    }

    fun setOnUpdateIceServersListener(listener: OnUpdateIceServersListener?) {
        if (listener == null) {
            onUpdateIceServersListener = listener
        } else {
            onUpdateIceServersListener = listener

            val iceServers = getIceServers()
            if (iceServers != null) {
                onUpdateIceServersListener?.onUpdate(iceServers)
            }
        }
    }

    fun getConfigs(): Configs? = configs

    fun setConfigs(configs: Configs): Boolean {
        this.configs = configs
        onUpdateConfigsListener?.onUpdate(configs)
        return this.configs == configs
    }

    fun getIceServers(): List<IceServer>? = iceServers

    fun setIceServers(iceServers: List<IceServer>): Boolean {
        this.iceServers = iceServers
        onUpdateIceServersListener?.onUpdate(iceServers)
        return this.iceServers == iceServers
    }

    fun destroy() {
        onUpdateConfigsListener = null
        onUpdateIceServersListener = null

        configs = null
        iceServers = null

        INSTANCE = null
    }

    fun interface OnUpdateConfigsListener {
        fun onUpdate(configs: Configs)
    }

    fun interface OnUpdateIceServersListener {
        fun onUpdate(iceServers: List<IceServer>)
    }

}