package q19.kenes_widget;

import android.content.Context;
import android.content.Intent;

import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity;

public class KenesWidget {

    public static class EntryParams {
        String hostname;

        public EntryParams(String hostname) {
            this.hostname = hostname;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }

    public static Intent open(Context context, EntryParams entryParams) {
        return KenesWidgetV2Activity.newIntent(context, entryParams.hostname);
    }

}
