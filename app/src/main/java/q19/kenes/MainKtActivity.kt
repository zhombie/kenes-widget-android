package q19.kenes

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SwitchCompat
import q19.kenes_widget.KenesWidget
import q19.kenes_widget.api.model.Authorization
import q19.kenes_widget.api.model.DeepLink
import q19.kenes_widget.api.model.Language

class MainKtActivity : AppCompatActivity() {

    private val tokenEditText by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<AppCompatEditText>(R.id.tokenEditText)
    }

    private val deepLinkSwitch by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<SwitchCompat>(R.id.deepLinkSwitch)
    }

    private val deepLinkActionView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<LinearLayout>(R.id.deepLinkActionView)
    }

    private val deepLinkActionValueView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.deepLinkActionValueView)
    }

    private val deepLinkPayloadEditText by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<AppCompatEditText>(R.id.deepLinkPayloadEditText)
    }

    private val openWidgetButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.openWidgetButton)
    }

    private var deepLinkAction: DeepLink.Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deepLinkActionView.setOnClickListener {
            AlertDialog.Builder(this)
                .setItems(arrayOf("None", "Calls screen", "Audio call", "Video call")) { dialog, which ->
                    dialog.dismiss()
                    deepLinkAction = when (which) {
                        0 -> null
                        1 -> DeepLink.Action.CALLS_SCREEN
                        2 -> DeepLink.Action.AUDIO_CALL
                        3 -> DeepLink.Action.VIDEO_CALL
                        else -> null
                    }

                    deepLinkActionValueView.text = when (deepLinkAction) {
                        DeepLink.Action.CALLS_SCREEN -> "Calls screen"
                        DeepLink.Action.AUDIO_CALL -> "Audio call"
                        DeepLink.Action.VIDEO_CALL -> "Video call"
                        else -> "None"
                    }
                }
                .show()
        }

        openWidgetButton?.setOnClickListener { openWidget() }
    }

    private fun openWidget() {
        /**
         * RU -> Для запуска виджета требуется отправить hostname.
         * Пример: https://kenes.vlx.kz
         *
         * EN -> To launch the widget, you need to send the hostname.
         * Example: https://kenes.vlx.kz
         */
        val bearerToken = tokenEditText?.text?.toString()
        val authorization = if (bearerToken.isNullOrBlank()) {
            null
        } else {
            Authorization(
                Authorization.Bearer(
                    token = bearerToken,
                    refreshToken = null,
                    scope = "scope:some:example",
                    expiresIn = 1234L
                )
            )
        }

        KenesWidget.Builder(this)
            .setHostname(DemonstrationConstants.HOSTNAME)
            .setLanguage(Language.RUSSIAN)
            .apply {
                if (authorization != null) {
                    setAuthorization(authorization)
                }
            }
            .apply {
                if (deepLinkSwitch.isChecked) {
                    val deepLinkAction = deepLinkAction
                    if (deepLinkAction != null) {
                        val payload = deepLinkPayloadEditText.text?.toString()
                        setDeepLink(DeepLink(deepLinkAction, payload))
                    }
                }
            }
            .launch()
    }

}