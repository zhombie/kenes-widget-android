package q19.kenes;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import q19.kenes_widget.KenesWidget;
import q19.kenes_widget.api.model.Authorization;
import q19.kenes_widget.api.model.Language;

class MainActivity extends AppCompatActivity {

    private AppCompatEditText tokenEditText;
    private Button openWidgetButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenEditText = findViewById(R.id.tokenEditText);
        openWidgetButton = findViewById(R.id.openWidgetButton);

        openWidgetButton.setOnClickListener(v -> openWidget());
    }

    private void openWidget() {
        /*
          RU -> Для запуска виджета требуется отправить hostname.
          Пример: https://kenes.vlx.kz

          EN -> To launch the widget, you need to send the hostname.
          Example: https://kenes.vlx.kz
         */
        KenesWidget.Builder builder = new KenesWidget.Builder(this)
            .setHostname(DemonstrationConstants.HOSTNAME)
            .setLanguage(Language.KAZAKH);

        String bearerToken = tokenEditText.getText().toString();
        if (bearerToken != null) {
            Authorization authorization = new Authorization(
                new Authorization.Bearer(
                    bearerToken,
                    null,
                    "scope:some:example",
                    1234L
                )
            );

            builder.setAuthorization(authorization);
        }

        builder.launch();
    }

}