package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import q19.kenes_widget.KenesWidget
import q19.kenes_widget.api.model.Authorization
import q19.kenes_widget.api.model.Language

class MainKtActivity : AppCompatActivity() {

    private val tokenEditText by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<AppCompatEditText>(R.id.tokenEditText)
    }

    private val openWidgetButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.openWidgetButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            .launch()
    }

}