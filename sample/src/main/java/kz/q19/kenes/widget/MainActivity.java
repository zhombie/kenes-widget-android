package kz.q19.kenes.widget;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import kz.q19.kenes.widget.api.Language;

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
        new KenesWidget.Builder(this)
            .setHostname(BuildConfig.HOSTNAME)
            .setLanguage(Language.RUSSIAN)
            .setImageLoader(new ConcatCoilImageLoader(this, BuildConfig.DEBUG))
            .launch();
    }

}