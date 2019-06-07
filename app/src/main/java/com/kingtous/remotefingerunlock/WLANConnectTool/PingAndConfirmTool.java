package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.content.Context;
import android.util.Log;

import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.subnet.Device;

import java.util.ArrayList;

public class PingAndConfirmTool {

    public static String findCorrectIP(Context context,RecordData data) {

        String IP = data.getIp();
        String MAC = data.getMac();
        String pingMAC;

        RecordData dataTmp=data;

        if (IP==null && MAC==null){
            return null;
        }
        if (MAC == null) {
            dataTmp.setMac(ARPInfo.getMACFromIPAddress(IP));
        }
        if (MAC != null && IP == null) {
            dataTmp.setIp(ARPInfo.getIPAddressFromMAC(MAC));
        }

        if (dataTmp.getIp() == null ) {
            return null;
        } else {
            if (dataTmp.getMac()==null){
                return IP;
            }
            else {
                pingMAC = ARPInfo.getMACFromIPAddress(dataTmp.getIp());
                if (pingMAC != null) {
                    if (pingMAC.equals(data.getMac())) {
                        return IP;
                    } else {
                        //mac不正确，寻找新的
                        String ip = ARPInfo.getIPAddressFromMAC(dataTmp.getMac());
                        if (ip == null) {
                            final String[] ipTmp = new String[1];
                            //搜索子网
                            SubnetDevices devices = SubnetDevices.fromLocalAddress();
                            final String finalMAC = data.getMac();
                            devices.findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                                @Override
                                public void onDeviceFound(Device device) {
                                    if (device.mac != null && device.mac.toUpperCase().equals(finalMAC)) {
                                        ipTmp[0] = device.ip;
                                    }
                                }

                                @Override
                                public void onFinished(ArrayList<Device> arrayList) {
                                }
                            });
                            if (ipTmp[0] == null) {
                                return IP;
                            } else {
                                ip = ipTmp[0];
                            }
                        }

                        dataTmp.setIp(ip.toUpperCase());
                        // 更新数据库
                        DataQueryHelper helper = new DataQueryHelper(context, context.getString(R.string.sqlDBName), null, 1);
                        if (RecordSQLTool.updatetoSQL(helper.getWritableDatabase(), data, dataTmp)) {
                            Log.d("数据库","更新");
                        }
                        return ip;
                    }
                }
                else {
                    return IP;
                }
            }

        }
    }

}
