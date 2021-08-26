package q19.kenes.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainKtActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openWidgetButton = findViewById<ExtendedFloatingActionButton>(R.id.openWidgetButton)

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
        val intent = KenesWidget.Builder()
            .setHostname(BuildConfig.HOSTNAME)
            .setLanguage(KenesWidget.Builder.Language.RUSSIAN)
            .build(this)
        startActivity(intent)
    }

}