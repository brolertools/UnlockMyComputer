package com.kingtous.remotefingerunlock.MenuTool;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.google.gson.JsonObject;
import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnect;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnectActivity;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;
import com.stealthcopter.networktools.WakeOnLan;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

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
            // 获取内网flag
            int flags=0;
            SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
            if (!preferences.getString(context.getString(R.string.connect_mode),"0").equals("0")) {
                flags = 1;
            }

            if (data.getMac()!=null && data.getIp()!=null) {
                if (WLANConnectActivity.isWifiConnected(context)) {
                    // 此时肯定是有MAC地址的，无MAC地址已经在UI界面就停止了
//                    final RecordData data1 = WLANConnect.checkIpEqualMac(context, data);
                    if (data != null && data.getIp() != null && data.getMac() != null) {
                        new Thread(new wake_on_lan(context,data,flags)).start();
                    }
                    else {
                        ToastMessageTool.ttl(context,"数据不符且无法自动调整，放弃发送");
                    }
                } else {
                    final int finalFlags = flags;
                    new AlertDialog.Builder(context)
                            .setMessage("检测到非WLAN网络，意味着无法访问内网，需要借助内网代理，是否继续...")
                            .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread(new wake_on_lan(context,data, finalFlags)).start();
                                }
                            })
                            .setNegativeButton("取消", null).show();
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
        int flags;

        wake_on_lan(Context context,RecordData data,int flags){
            this.context=context;
            this.data=data;
            this.flags=flags;
        }

        @Override
        public void run() {
            Looper.prepare();
            int result=-1;
            try {
                if (flags==0) {
                    WakeOnLan.sendWakeOnLan(data.getIp(), data.getMac());
                    //全网广播
                    WakeOnLan.sendWakeOnLan(InetAddress.getByName("255.255.255.255"), data.getMac(), 9, 10000, 5);
                    result = 0;
                }
                else {
                    // 内网模式
                    try {
                        Socket socket=SSLSecurityClient.CreateSocket(context,context.getString(R.string.nat_server), WLANDeviceData.nat_wake_on_lan);
                        if (socket!=null){
                            OutputStream stream=socket.getOutputStream();
                            if (stream!=null){
                                JsonObject object=new JsonObject();
                                object.addProperty("oriMac", FunctionTool.macAddressAdjust(data.getMac()));
                                object.addProperty("ip",data.getIp());
                                stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                                stream.close();
                                socket.close();
                                result = 0;
                            }
                        }
                    }catch (SocketException e){
                        ToastMessageTool.ttl(context, "未发送，请检查与服务器的连接");
                    }
                }
                if (result==0)
                    ToastMessageTool.ttl(context, "已发送Wake On LAN数据包，请自行检查远程端是否接收到");
            } catch (IOException e) {
                ToastMessageTool.ttl(context, e.getMessage());
            }
            Looper.loop();
        }
    }

}
