package q19.kenes.widget;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openWidgetButton = findViewById(R.id.openWidgetButton);

        openWidgetButton.setOnClickListener(v -> openWidget());
    }

    private void openWidget() {
        /*
          RU -> Для запуска виджета требуется отправить hostname.
          Пример: https://kenes.vlx.kz

          EN -> To launch the widget, you need to send the hostname.
          Example: https://kenes.vlx.kz
         */
        Intent intent = new KenesWidget.Builder("https://kenes.vlx.kz")
                .setLanguage(KenesWidget.Builder.Language.RU)
                .build(this);
        startActivity(intent);
    }

}