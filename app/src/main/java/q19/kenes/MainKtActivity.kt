package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidget

class MainKtActivity : AppCompatActivity() {

    private val openWidget by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.openWidget)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openWidget()

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
        startActivity(
            KenesWidget.open(
                this,
                KenesWidget.EntryParams(
                    DemonstrationConstants.HOSTNAME,
                    language = DemonstrationConstants.LANGUAGE
                )
            )
        )
    }

}