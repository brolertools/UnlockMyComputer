package com.kingtous.remotefingerunlock.WLANConnectTool;

public class WLANDeviceData {

    public static int unlock_port = 2084;
    public static int transfer_port = 2090;
    public static int nat_transfer_port=2071;
    public static int nat_wake_on_lan=2072;
    public static int nat_unlock_port = 2073;
    public static int unlock_port_tmp=8970;

    String name;
    String mac;
    String ip;

    public WLANDeviceData(String name, String mac, String ip) {
        if (name == null || name.equals("")) {
            this.name = "(未指定)";
        }
        this.name = name;
        this.mac = mac;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
