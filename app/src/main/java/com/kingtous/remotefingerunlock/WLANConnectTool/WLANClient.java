package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.ScriptGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.FileTransferTool.FileModel;
import com.kingtous.remotefingerunlock.FileTransferTool.FileTransferActivity;
import com.kingtous.remotefingerunlock.FileTransferTool.SocketHolder;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

import androidx.appcompat.app.AlertDialog;

public class WLANClient extends AsyncTask<Void,String,Void> {

    String host;
    int port;
    int flags;
    RecordData data;
    private Context context;
    String message;
    int resultCode = -1;

    String pre_message;


    // ===========
    ProgressDialog dialog;

    WLANClient(Context context, String host, int port, RecordData data, int flags) {
        this.context = context;
        this.host = host;
        this.port = port;
        this.data = data;
        this.flags = flags;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        dialog.setMessage("正在连接，请稍后");
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel(true);
            }
        });
        dialog.setCancelable(false);
        if (context!=context.getApplicationContext())
            dialog.show();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        for (String str : values) {
            dialog.setMessage(str);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        dialog.dismiss();
        if (context!=context.getApplicationContext()){
            new AlertDialog.Builder(context)
                    .setMessage(pre_message)
                    .setPositiveButton("确定",null)
                    .show();
        }
        else{
            ToastMessageTool.ttl(context,pre_message);
        }
    }

    private void log(String text) {
        if (context != null) {
            if (context != context.getApplicationContext()) {
                publishProgress(new String[]{text});
            }
            pre_message=text;
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            Ping p = Ping.onAddress(host);
            p.setTimeOutMillis(1000);
            PingResult result = p.doPing();
//弃用            if (!result.isReachable() || (macinfo!=null && !ARPInfo.getMACFromIPAddress(host).equals(data.getMac()))) {
            if (flags == 0) {
                if (!result.isReachable()) {
                    if (data.getMac().equals("") || data.getMac() == null) {
                    } else {
                        log("IP已更改，正在重新查找");
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
                                //TODO 后期更新，当WiFi无法ping通时调用蓝牙
                                log("无法建立连接，请检查设备是否开启服务端");
                                return null;
                            } else {
                                ip = ipTmp[0];
                                host = ip;
                            }
                        }
                        dataTmp.setIp(ip.toUpperCase());
                        // 更新数据库
                        DataQueryHelper helper = new DataQueryHelper(context, context.getString(R.string.sqlDBName), null, 1);
                        if (RecordSQLTool.updatetoSQL(helper.getWritableDatabase(), data, dataTmp)) {
                            log("IP变化，已更新数据");
                        }
                    }
                }
            }
            FileTransferActivity.CreateSocket(context, host);
            Socket socket = SSLSecurityClient.CreateSocket(context, host, port);//new Socket(host,unlock_port);//SSLSecurityClient.CreateSocket(context,host,unlock_port);
            if (socket == null) {
                log("无法建立连接，请检查设备是否开启服务端");
                return null;
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
            OutputStream stream = socket.getOutputStream();
            JSONObject object = new JSONObject();
            object.put("oriMac", FunctionTool.macAddressAdjust(data.getMac()));
            object.put("username", data.getUser());
            object.put("passwd", data.getPasswd());
            stream.write(object.toString().getBytes(StandardCharsets.UTF_8));

            if (flags == 1) {
                // 服务器返回信息
                BufferedInputStream buffered = new BufferedInputStream(socket.getInputStream());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int r = -1;
                byte buff[] = new byte[1024];
                while ((r = buffered.read(buff, 0, 1024)) != -1) {
                    byteArrayOutputStream.write(buff, 0, r);
//                            if(buffered.available() <=0) //添加这里的判断
//                            {
//                                break;
//                            }
                }
                socket.close();
                String recvStr = new String(byteArrayOutputStream.toByteArray());
                JsonObject object1 = new Gson().fromJson(recvStr, JsonObject.class);

//                        return new Gson().fromJson(object1,FileModel.class);
                if (object1==null || !object1.has("status")) {
                    throw new IOException("未返回状态码");
                }

                if (object1.get("status").getAsString().equals("0")) {
                    message = recvStr;
                    resultCode = 0;
                } else {
                    switch (object1.get("status").getAsString()) {
                        case "-1":
                            throw new IOException("权限错误");
                        case "-2":
                            throw new IOException("设备已离线");
                        default:
                            throw new IOException("未知错误");
                    }
                }

            }
            stream.close();
            log("远程设备端已接收到请求");
        } catch (IOException e) {
            log("未获取到设备的返回情况\n" + e.getMessage());
        } catch (JSONException e) {
            log("数据异常\n" + e.getMessage());
        }
        return null;
    }
}
