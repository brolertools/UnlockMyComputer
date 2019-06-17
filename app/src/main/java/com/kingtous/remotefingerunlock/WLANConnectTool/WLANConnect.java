package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.subnet.Device;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;

import androidx.appcompat.app.AlertDialog;

public class WLANConnect {


    private WifiManager manager;
    private Context context;
    private RecordData data;

    public WLANConnect(Context context, RecordData data) {
        manager = (WifiManager) context.getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        this.context = context;
        this.data = data;
    }

    public void start() {
        if (manager != null) {
            checkAndConnectWLAN(context, Objects.requireNonNull(manager));
        }
    }

    private void checkAndConnectWLAN(final Context context, final WifiManager manager) {
        if (!manager.isWifiEnabled()) {
            new AlertDialog.Builder(context)
                    .setMessage("未打开WLAN，请问是否开启?")
                    .setPositiveButton("开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            manager.setWifiEnabled(true);
                        }
                    })
                    .setNegativeButton("使用其他网络(数据)", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startConnect(context,data);
                        }
                    })
                    .show();

        } else {
            startConnect(context, data);
        }

    }


    private void startConnect(Context context, RecordData data) {
        WLANClient client;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getString(context.getString(R.string.connect_mode),"0").equals("0")) {
            client = new WLANClient(context, data.getIp(), WLANDeviceData.unlock_port, data,0);
        }
        else{
            client = new WLANClient(context, context.getString(R.string.nat_server), WLANDeviceData.nat_transfer_port, data,1);
        }
        Toast.makeText(context, "正在连接中", Toast.LENGTH_LONG).show();
        client.start();
    }


    public static RecordData checkIpEqualMac(Context context, final RecordData data) {

        Ping p = Ping.onAddress(data.getIp());
        p.setTimeOutMillis(1000);
        PingResult result = null;
        try {
            result = p.doPing();
            String macinfo = ARPInfo.getMACFromIPAddress(data.getIp());
            if (!result.isReachable() || (macinfo != null && !macinfo.equals(data.getIp()))) {

                if (data.getMac().equals("") || data.getMac() == null) {
                    return null;
                } else {
                    ToastMessageTool.ttl(context, "mac与ip不匹配，正在重新搜索");
                }
                // 尝试用Mac搜索新的host
                RecordData dataTmp = data;
                String ip = ARPInfo.getIPAddressFromMAC(dataTmp.getMac());
                if (ip == null) {
                    final String[] ipTmp = new String[1];
                    //搜索子网
                    SubnetDevices devices = SubnetDevices.fromLocalAddress();
                    devices.findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                        @Override
                        public void onDeviceFound(Device device) {
                            if (device.mac != null && device.mac.toUpperCase().equals(data.getMac())) {
                                ipTmp[0] = device.ip;
                            }
                        }
                        @Override
                        public void onFinished(ArrayList<Device> arrayList) {
                        }
                    });
                    if (ipTmp[0] == null) {
                        ToastMessageTool.ttl(context, "无法建立连接，请检查设备是否开启服务端");
                        return null;
                    } else {
                        ip = ipTmp[0];
                    }
                }
                dataTmp.setIp(ip.toUpperCase());
                // 更新数据库
                DataQueryHelper helper = new DataQueryHelper(context, context.getString(R.string.sqlDBName), null, 1);
                if (RecordSQLTool.updatetoSQL(helper.getWritableDatabase(), data, dataTmp)) {
                    ToastMessageTool.ttl(context, "IP变化，已更新数据");
                }
                return dataTmp;
            } else {
//                if (macinfo == null) {
//                    ToastMessageTool.tts(context, "动态监测IP失败，使用默认IP地址");
//                }
                return data;
            }
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
        return null;
    }


}
