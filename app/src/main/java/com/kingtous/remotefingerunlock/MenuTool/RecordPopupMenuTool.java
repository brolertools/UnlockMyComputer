package com.kingtous.remotefingerunlock.MenuTool;

import android.content.Context;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnect;
import com.stealthcopter.networktools.WakeOnLan;

import java.io.IOException;

import androidx.annotation.NonNull;

public class RecordPopupMenuTool {

    public static PopupMenu createInstance(@NonNull Context context, @NonNull View view){
        PopupMenu popupMenu=new PopupMenu(context,view);
        Menu menu=popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.record_more_menu,menu);
        return popupMenu;
    }

    public static void wakeOnLAN(final Context context, RecordData data){
        if (data.getType().equals("WLAN")){
            if (data.getMac()!=null && data.getIp()!=null) {
                final RecordData data1 = WLANConnect.checkIpEqualMac(context, data);
                if (data1 != null && data1.getIp()!=null && data1.getMac()!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            try {
                                WakeOnLan.sendWakeOnLan(data1.getIp(), data1.getMac());
                                ToastMessageTool.ttl(context, "已发送Wake On LAN数据包");
                            } catch (IOException e) {
                                ToastMessageTool.ttl(context, e.getMessage());
                            }
                            Looper.loop();
                        }
                    }).start();
                }
            }
        }
        else ToastMessageTool.ttl(context,"非WLAN记录，不能使用Wake On LAN");

    }

}
