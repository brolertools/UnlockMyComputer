package com.kingtous.remotefingerunlock.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.kingtous.remotefingerunlock.Common.Connect;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;

/**
 * Implementation of App Widget functionality.
 */
public class UnlockWidget extends AppWidgetProvider {


    static DataQueryHelper helper;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
//        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unlock_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
//        views.setOnClickPendingIntent(R.id.appwidget_text,getPendingIntent(context,R.id.appwidget_text));
        RemoteViews views= updateViewMethod(helper,context);
        views.setOnClickPendingIntent(R.id.appwidget,getPendingIntent(context,R.id.appwidget));
//        context.startService(new Intent(context, UnlockWidgetService.class));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    public static void update(Context context){
            if (helper==null){
                helper=new DataQueryHelper(context,context.getString(R.string.sqlDBName),null,1);
            }
            RemoteViews views= updateViewMethod(helper,context);
            views.setOnClickPendingIntent(R.id.appwidget_text,getPendingIntent(context,R.id.appwidget_text));
//        context.startService(new Intent(context, UnlockWidgetService.class));
            // Instruct the widget manager to update the widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(new ComponentName(context, UnlockWidget.class), views);
    }

    public static RemoteViews updateViewMethod(DataQueryHelper helper, Context context){
        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.unlock_widget);
        if (helper==null){
            helper=new DataQueryHelper(context,context.getString(R.string.sqlDBName),null,1);
        }
        SQLiteDatabase d=helper.getWritableDatabase();
        RecordData defaultRecordData= RecordSQLTool.getDefaultRecordData(d);
        if (defaultRecordData!=null){
            views.setTextViewText(R.id.appwidget_user,"用户名: "+defaultRecordData.getUser());
            views.setTextViewText(R.id.appwidget_method,"解锁方式: "+defaultRecordData.getType());
        }
        else {
            views.setTextViewText(R.id.appwidget_text, context.getString(R.string.appwidget_text));
        }
        d.close();
        return views;
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Uri data = intent.getData();
        int resId = -1;
        if (data!=null){
            resId = Integer.parseInt(data.getSchemeSpecificPart());
        }
        switch (resId){
            case R.id.appwidget:
                if (helper==null){
                    helper=new DataQueryHelper(context,context.getString(R.string.sqlDBName),null,1);
                }
                SQLiteDatabase d=helper.getReadableDatabase();
                RecordData defaultRecordData= RecordSQLTool.getDefaultRecordData(d);
                if (defaultRecordData!=null)
                {
                    //检测是否开启WiFi，因为小控件无法弹出窗口
                    if (defaultRecordData.getType().equals("WLAN")){
                        WifiManager manager= (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo info=manager.getConnectionInfo();
                        SupplicantState state=info.getSupplicantState();
                        if (state.name().equals("DISCONNECTED")){
                            Toast.makeText(context,"WiFi未连接...",Toast.LENGTH_SHORT).show();
                            return;
                        }

                    }
                    else if (defaultRecordData.getType().equals("Bluetooth")){
                        BluetoothManager manager= (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                        BluetoothAdapter adapter=manager.getAdapter();
                        if (!adapter.isEnabled()){
                            Toast.makeText(context,"蓝牙未连接...",Toast.LENGTH_SHORT).show();
                            return;
                        }

                    }

                    Toast.makeText(context,"验证成功，连接中...",Toast.LENGTH_SHORT).show();
                    Connect.start(context.getApplicationContext(),defaultRecordData);
                }
                else Toast.makeText(context,"还未配置默认连接，请到设置中配置或在扫描时添加默认配置",Toast.LENGTH_LONG).show();
                d.close();
                break;
        }
    }

    private static PendingIntent getPendingIntent(Context context, int resID){
        Intent intent = new Intent();
        intent.setClass(context, UnlockWidget.class);//如果没有这一句，表示匿名的。加上表示是显式的。在单个按钮的时候是没啥区别的，但是多个的时候就有问题了
        intent.setAction("unlock");
        //设置data域的时候，把控件id一起设置进去，
        // 因为在绑定的时候，是将同一个id绑定在一起的，所以哪个控件点击，发送的intent中data中的id就是哪个控件的id
        intent.setData(Uri.parse("id:" + resID));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,intent,0);
        return pendingIntent;
    }

}

