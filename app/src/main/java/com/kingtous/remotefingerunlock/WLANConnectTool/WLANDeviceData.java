package com.kingtous.remotefingerunlock.WLANConnectTool;

public class WLANDeviceData {

    // 直连模式
    public static int unlock_port = 2084;
    public static int transfer_port = 2090;
    public static int unlock_udp_port =8970;
    public static int heart_beat_port=2076;
    // http代理
    public static int nat_transfer_port=2072;
    public static int nat_wake_on_lan=2073;
    public static int nat_unlock_port = 2075;

    private String name;
    private String mac;
    private String ip;

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
