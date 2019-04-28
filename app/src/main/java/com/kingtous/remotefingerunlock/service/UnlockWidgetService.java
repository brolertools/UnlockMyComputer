package com.kingtous.remotefingerunlock.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Widget.UnlockWidget;

import androidx.annotation.Nullable;

/*
预留服务端
 */
public class UnlockWidgetService extends Service {

    private static final int ALARM_DURATION  = 5 * 60 * 1000; // service 自启间隔
    private static final int UPDATE_DURATION = 10 * 1000;     // Widget 更新间隔
    private static final int UPDATE_MESSAGE  = 1000;

    private UpdateHandler updateHandler; // 更新 Widget 的 Handler

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每个 ALARM_DURATION 自启一次
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(getBaseContext(), UnlockWidgetService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_DURATION, pendingIntent);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateHandler=new UpdateHandler();
        Message message = updateHandler.obtainMessage();
        message.what = UPDATE_MESSAGE;
        updateHandler = new UpdateHandler();
        updateHandler.sendMessageDelayed(message, UPDATE_DURATION);
    }


    private void updateWidget() {
        Log.d("更新:","widget");
//        Toast.makeText(getApplicationContext(),"更新控件",Toast.LENGTH_SHORT).show();
        // 更新 Widget
        RemoteViews remoteViews = UnlockWidget.updateViewMethod(new DataQueryHelper(getApplicationContext(),getString(R.string.sqlDBName),null,1),
                getApplicationContext());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetManager.updateAppWidget(new ComponentName(getApplicationContext(), UnlockWidget.class), remoteViews);

        // 发送下次更新的消息
        Message message = updateHandler.obtainMessage();
        message.what = UPDATE_MESSAGE;
        updateHandler.sendMessageDelayed(message, UPDATE_DURATION);
    }

    protected final class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_MESSAGE:
                    updateWidget();
                    break;
                default:
                    break;
            }
        }
    }
}
