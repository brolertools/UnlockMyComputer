package com.kingtous.remotefingerunlock.MenuTool;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnect;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnectActivity;
import com.stealthcopter.networktools.WakeOnLan;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class RecordPopupMenuTool {

    public static PopupMenu createInstance(@NonNull Context context, @NonNull View view){
        PopupMenu popupMenu=new PopupMenu(context,view);
        Menu menu=popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.record_more_menu,menu);
        return popupMenu;
    }

    public static void wakeOnLAN(final Context context, final RecordData data){
        if (data.getType().equals("WLAN")){
            if (data.getMac()!=null && data.getIp()!=null) {
                if (WLANConnectActivity.isWifiConnected(context)) {
                    // 此时肯定是有MAC地址的，无MAC地址已经在UI界面就停止了
//                    final RecordData data1 = WLANConnect.checkIpEqualMac(context, data);
                    if (data != null && data.getIp() != null && data.getMac() != null) {
                        new Thread(new wake_on_lan(context,data)).start();
                    }
                    else {
                        ToastMessageTool.ttl(context,"数据不符且无法自动调整，放弃发送");
                    }
                } else {
                    new AlertDialog.Builder(context)
                            .setMessage("检测到非WLAN网络，请问接下来...")
                            .setPositiveButton("直接连接", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread(new wake_on_lan(context,data)).start();
                                }
                            })
                            .setNegativeButton("内网连接", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO 内网接口
                                }
                            }).show();
                }
            }
        }
        else {
            ToastMessageTool.ttl(context,"非WLAN记录，不能使用Wake On LAN");
        }

    }


    private static class wake_on_lan implements Runnable{

        Context context;
        RecordData data;

        wake_on_lan(Context context,RecordData data){
            this.context=context;
            this.data=data;
        }

        @Override
        public void run() {
            Looper.prepare();
            try {
                WakeOnLan.sendWakeOnLan(data.getIp(), data.getMac());
                ToastMessageTool.ttl(context, "已发送Wake On LAN数据包，请自行检查远程端是否接收到");
            } catch (IOException e) {
                ToastMessageTool.ttl(context, e.getMessage());
            }
            Looper.loop();
        }
    }

}
