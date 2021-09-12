package kz.q19.kenes.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kz.q19.kenes.widget.api.Language

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
        KenesWidget.Builder(this)
            .setHostname(BuildConfig.HOSTNAME)
            .setLanguage(Language.RUSSIAN)
            .setImageLoader(ConcatCoilImageLoader(this, BuildConfig.DEBUG))
            .launch()
    }

}