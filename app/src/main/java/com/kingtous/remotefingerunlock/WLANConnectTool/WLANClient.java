package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.ScriptGroup;
import android.widget.Toast;

import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.Security.SSLSecurityDoubleClient;
import com.kingtous.remotefingerunlock.Security.VersionChecker;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.subnet.Device;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WLANClient extends Thread {

    String host;
    int port;
    RecordData data;
    private Context context;

    WLANClient(Context context,String host, int port, RecordData data) {
        this.context=context;
        this.host = host;
        this.port = port;
        this.data = data;
    }

    @Override
    public void run() {
        Looper.prepare();
        try {
            Ping p=Ping.onAddress(host);
            p.setTimeOutMillis(1000);
            PingResult result=p.doPing();


            if (!result.isReachable() || !ARPInfo.getMACFromIPAddress(host).equals(data.getMac())){

                if (data.getMac().equals("") || data.getMac()==null){

                }
                else {
                    log("IP已更改，正在重新查找");
                    // 尝试用Mac搜索新的host
                    RecordData dataTmp=data;
                    String ip=ARPInfo.getIPAddressFromMAC(dataTmp.getMac());
                    if (ip==null){
                        final String[] ipTmp = new String[1];
                        //搜索子网
                        SubnetDevices devices=SubnetDevices.fromLocalAddress();
                        devices.findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                            @Override
                            public void onDeviceFound(Device device) {
                                if (device.mac!=null && device.mac.toUpperCase().equals(data.getMac())){
                                    ipTmp[0] =device.ip;
                                }
                            }
                            @Override
                            public void onFinished(ArrayList<Device> arrayList) {
                            }
                        });
                        if (ipTmp[0]==null){
                            //TODO 后期更新，当WiFi无法ping通时调用蓝牙
                            log("无法建立连接，请检查设备是否开启服务端");
                            return;
                        }
                        else {
                            ip=ipTmp[0];
                            host=ip;
                        }

                    }
                    dataTmp.setIp(ip.toUpperCase());
                    // 更新数据库
                    DataQueryHelper helper=new DataQueryHelper(context,context.getString(R.string.sqlDBName),null,1);
                    if (RecordSQLTool.updatetoSQL(helper.getWritableDatabase(),data, dataTmp)){
                        log("IP变化，已更新数据");
                    }
                }
            }
            Socket socket= SSLSecurityClient.CreateSocket(context,host,port);//new Socket(host,port);//SSLSecurityClient.CreateSocket(context,host,port);
            if (socket==null){
                log("无法建立连接，请检查设备是否开启服务端");
                return;
            }
//            InputStream istream=socket.getInputStream();
//            if (istream!=null){
//                InputStreamReader reader=new InputStreamReader(istream);
//                BufferedReader br=new BufferedReader(reader);
//                String version=br.readLine();
//                if (!VersionChecker.versionAvaliable(version)){
//                    log("请升级远程设备版本，版本号为"+VersionChecker.versionRequirement);
//                    return;
//                }
//            }
            OutputStream stream=socket.getOutputStream();
            JSONObject object = new JSONObject();
            object.put("username", data.getUser());
            object.put("passwd", data.getPasswd());
            stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
            stream.close();
            log("远程设备端已接收到请求");
        } catch (IOException ignored) {
            log("设备未准备好\n"+ignored.getMessage());
        } catch (JSONException ignored) {
            log("数据异常\n"+ignored.getMessage());
        }
        finally {
            Looper.loop();
        }

    }

    private void log(String text){
        if (context!=null){
            Toast.makeText(context,text,Toast.LENGTH_LONG).show();
        }
    }

}
