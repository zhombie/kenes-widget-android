package q19.kenes;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity;

class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openWidget = findViewById(R.id.openWidget);

        openWidget();

        openWidget.setOnClickListener(v -> openWidget());
    }

    private void openWidget() {
        startActivity(KenesWidgetV2Activity.newIntent(this, "https://help.post.kz"));
    }

}