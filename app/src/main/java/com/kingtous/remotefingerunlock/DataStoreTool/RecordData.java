package com.kingtous.remotefingerunlock.DataStoreTool;

import com.kingtous.remotefingerunlock.Security.SecurityTransform;

public class RecordData {

    private String name;
    private String type;
    private String user;
    private String passwd;
    private String ip;
    //蓝牙的mac即mac，WLAN连接则为ip地址
    private String mac;
    private int isDefault;
    public static int FALSE=0;
    public static int TRUE=1;

    public RecordData(){

    }

    public RecordData(String Type,String name,String User,String Passwd,String ip,String Mac)
    {
        this.name=name;
        this.type=Type;
        this.user=User;
        this.ip=ip;
        this.passwd=SecurityTransform.encrypt(Passwd);
        this.mac=Mac;
        this.isDefault=FALSE;
    }

    public RecordData(String Type,String name,String User,String Passwd,String ip,String Mac,int isDefault)
    {
        this.name=name;
        this.type=Type;
        this.user=User;
        this.ip=ip;
        this.passwd=SecurityTransform.encrypt(Passwd);
        this.mac=Mac;
        this.isDefault=isDefault;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPasswd() {
        return SecurityTransform.decrypt(passwd);
    }

    public void setPasswd(String passwd) {
        this.passwd = SecurityTransform.encrypt(passwd);
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(int isDefault) {
        this.isDefault = isDefault;
    }
}
