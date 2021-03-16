package q19.kenes;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import q19.kenes_widget.KenesWidget;
import q19.kenes_widget.api.model.Authorization;
import q19.kenes_widget.api.model.Language;
import q19.kenes_widget.api.model.User;

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
        new KenesWidget.Builder(this)
            .setHostname(DemonstrationConstants.HOSTNAME)
            .setLanguage(Language.KAZAKH)
            .setAuthorization(
                new Authorization(
                    new Authorization.Bearer(
                        "b29a2cfb-cd04-4131-bbf8-ffc216747cbb",
                        null,
                        "scope:some:example",
                        1234L
                    )
                )
            )
            .setUser(
                new User.Builder()
                    .setPhoneNumber("77771234567")
                    .build()
            )
            .launch();
    }

}