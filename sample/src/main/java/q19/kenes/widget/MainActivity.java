package q19.kenes.widget;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExtendedFloatingActionButton openWidgetButton = findViewById(R.id.openWidgetButton);

        openWidgetButton.setOnClickListener(v -> openWidget());
    }

    private void openWidget() {
        /*
          RU -> Для запуска виджета требуется отправить hostname.
          Пример: https://kenes.vlx.kz

          EN -> To launch the widget, you need to send the hostname.
          Example: https://kenes.vlx.kz
         */
        Intent intent = new KenesWidget.Builder()
            .setHostname(BuildConfig.HOSTNAME)
            .setLanguage(KenesWidget.Builder.Language.RUSSIAN)
            .build(this);
        startActivity(intent);
    }

}