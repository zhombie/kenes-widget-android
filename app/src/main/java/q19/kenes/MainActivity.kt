package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidgetV2Activity

class MainActivity : AppCompatActivity() {

    private val openWidget by lazy {
        findViewById<Button>(R.id.openWidget)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openWidget()

        openWidget?.setOnClickListener { openWidget() }
    }

    private fun openWidget() {
        startActivity(KenesWidgetV2Activity.newIntent(this, "https://kenes.1414.kz"))
    }

}
