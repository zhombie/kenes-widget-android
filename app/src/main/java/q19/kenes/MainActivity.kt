package q19.kenes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidgetV2Activity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivity(KenesWidgetV2Activity.newIntent(this))
    }

}
