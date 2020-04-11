package q19.kenes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.Constants
import q19.kenes_widget.WidgetActivity


class MainActivity : AppCompatActivity() {

    private var isChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.openWidget)?.setOnClickListener {
            val intent = Intent(this, WidgetActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.changeUrl)?.setOnClickListener {
            if (isChanged) {
                Constants.URL = "https://kenes.vlx.kz/admin/widget?is_mobile=true"
                isChanged = false
            } else {
                Constants.URL = "https://webrtc.github.io/samples/src/content/getusermedia/gum/"
                isChanged = true
            }
        }
    }

}
