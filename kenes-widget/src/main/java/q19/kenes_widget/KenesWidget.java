package q19.kenes_widget;

import android.content.Context;
import android.content.Intent;

import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity;

public class KenesWidget {

    Context context;
    String hostname;

    public KenesWidget(Context context, String hostname) {
        this.context = context;
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Intent open() {
        return KenesWidgetV2Activity.newIntent(context, hostname);
    }

}
