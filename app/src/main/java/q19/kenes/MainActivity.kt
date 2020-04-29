package q19.kenes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesVideoCallActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivity(KenesVideoCallActivity.newIntent(this))
    }

}
