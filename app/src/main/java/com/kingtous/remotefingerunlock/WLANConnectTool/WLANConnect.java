package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;

import java.util.Objects;

import androidx.appcompat.app.AlertDialog;

public class WLANConnect {


    private WifiManager manager;
    private Context context;
    private RecordData data;

    public WLANConnect(Context context, RecordData data){
        manager=(WifiManager) context.getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        this.context=context;
        this.data=data;
    }

    public void start(){
        if (manager!=null){
            checkAndConnectWLAN(context, Objects.requireNonNull(manager));
        }
    }


    private void checkAndConnectWLAN(final Context context, final WifiManager manager){
        if (!manager.isWifiEnabled()){
            new AlertDialog.Builder(context)
                    .setMessage("未打开WLAN，请问是否开启?")
                    .setPositiveButton("开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            manager.setWifiEnabled(true);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();

        }
        else {
            startConnect(context,data);
        }

    }


    private void startConnect(Context context,RecordData data){
        WLANClient client=new WLANClient(context,data.getMac(),WLANDeviceData.port,data);
        Toast.makeText(context,"正在连接中",Toast.LENGTH_LONG).show();
        client.execute();
    }



}
