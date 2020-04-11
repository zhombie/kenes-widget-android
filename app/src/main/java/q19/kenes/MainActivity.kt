package q19.kenes

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.KenesWidgetActivity
import q19.kenes_widget.Project


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.openWidget)?.setOnClickListener {
//            val intent = Intent(this, KenesWidgetActivity::class.java)
            startActivity(KenesWidgetActivity.newIntent(this, Project.KPT))
        }
    }

}
