package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPReceiever extends AsyncTask<Void,Void, Void >{

    public interface ReturnListener{
        void onReturnListener(WLANDeviceData data);
    }

    public ReturnListener mReturnListener;
    private WLANDeviceData wlanDeviceData;

    public void setmReturnListener(ReturnListener listener){
        this.mReturnListener=listener;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mReturnListener.onReturnListener(wlanDeviceData);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            while (true){
                DatagramSocket socket=new DatagramSocket(WLANDeviceData.unlock_udp_port);
                socket.setSoTimeout(3000);
                //创建字节数组以做数据缓冲区
                byte[] words=new byte[1024];
                //创建DatagramPacket类对象，并调用构造器用来接收长度为 length 的数据包
                DatagramPacket dp=new DatagramPacket(words,0,words.length);
                //调用DatagramSocket类方法receive()接收数据报包
                socket.receive(dp);
                //再将数据报包转换成字节数组
                byte[] data=dp.getData();
                String ip=dp.getAddress().getHostAddress();
                //通过使用平台的默认字符集解码data字节数组,方便打印输入
                String str=new String(data,0,data.length);
                str=str.substring(0,str.indexOf("\n"));
                JsonObject object=new Gson().fromJson(str, JsonObject.class);

                if (object.has("pcname") && object.has("macaddr")){
                    //处理mac地址,加冒号
                    String macTmp="";
                    String mac=object.get("macaddr").getAsString();
                    for (int index=0;index<mac.length();index=index+2){
                        macTmp=macTmp+mac.substring(index,index+2).toUpperCase()+":";
                    }
                    macTmp=macTmp.substring(0,macTmp.length()-1);

                    wlanDeviceData=new WLANDeviceData(
                            object.get("pcname").getAsString(),
                            macTmp,
                            ip);

                    return null;
                }

            }

        } catch (SocketException ignored) {

        } catch (IOException ignored) {

        }catch (JsonSyntaxException ignored){

        }
        return null;
    }
}
