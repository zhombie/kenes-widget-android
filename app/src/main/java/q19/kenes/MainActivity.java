package q19.kenes;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import q19.kenes_widget.KenesWidget;

class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openWidget = findViewById(R.id.openWidget);

        openWidget.setOnClickListener(v -> openWidget());
    }

    private void openWidget() {
        /*
          RU -> Для запуска виджета требуется отправить hostname.
          Пример: https://kenes.vlx.kz

          EN -> To launch the widget, you need to send the hostname.
          Example: https://kenes.vlx.kz
         */
        startActivity(KenesWidget.open(this, new KenesWidget.EntryParams(
                DemonstrationConstants.HOSTNAME,
                DemonstrationConstants.INSTANCE.getLANGUAGE())));
    }

}