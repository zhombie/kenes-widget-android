package q19.kenes_widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class KenesVideoCallActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, KenesVideoCallActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_video_call)
    }

}