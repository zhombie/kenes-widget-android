package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidget
import q19.kenes_widget.api.model.Authorization
import q19.kenes_widget.api.model.Language
import q19.kenes_widget.api.model.User

class MainKtActivity : AppCompatActivity() {

    private val openWidget by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.openWidget)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openWidget?.setOnClickListener { openWidget() }
    }

    private fun openWidget() {
        /**
         * RU -> Для запуска виджета требуется отправить hostname.
         * Пример: https://kenes.vlx.kz
         *
         * EN -> To launch the widget, you need to send the hostname.
         * Example: https://kenes.vlx.kz
         */
        KenesWidget.Builder(this)
            .setHostname(DemonstrationConstants.HOSTNAME)
            .setLanguage(Language.RUSSIAN)
            .setAuthorization(
                Authorization(
                    Authorization.Bearer(
                        token = "b29a2cfb-cd04-4131-bbf8-ffc216747cbb",
                        refreshToken = null,
                        scope = "scope:some:example",
                        expiresIn = 1234L
                    )
                )
            )
            .setUser(
                User.Builder()
                    .setIIN("112233445566")
                    .setPhoneNumber("77771234567")
                    .build()
            )
            .launch()
    }

}