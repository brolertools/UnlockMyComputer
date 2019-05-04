package com.kingtous.remotefingerunlock.Common;

import android.content.Context;
import android.widget.Toast;

public class ToastMessageTool {

    public static void tts(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void ttl(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}
