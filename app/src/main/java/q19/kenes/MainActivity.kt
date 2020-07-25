package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidget
import q19.kenes_widget.model.EntryParams

class MainActivity : AppCompatActivity() {

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
        startActivity(KenesWidget.open(this, EntryParams(hostname = "https://kenes.vlx.kz")))
    }

}
