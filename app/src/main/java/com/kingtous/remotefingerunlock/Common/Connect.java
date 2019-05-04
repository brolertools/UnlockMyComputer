package com.kingtous.remotefingerunlock.Common;

import android.content.Context;

import com.kingtous.remotefingerunlock.BluetoothConnectTool.BluetoothConnect;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnect;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

public class Connect {

    public static void start(Context context, RecordData data) {
        if (data != null) {
            if (data.getType().equals("Bluetooth")) {
                BluetoothConnect connection = new BluetoothConnect();
                connection.start(context, data);
            } else {
                WLANConnect connection = new WLANConnect(context, data);
                connection.start();
            }
        } else return;
    }
}
