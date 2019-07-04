package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.WLANConnectTool.PingAndConfirmTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileTransferConnectTask extends AsyncTask<RecordData, String, Void> implements DialogInterface.OnClickListener{

    private Context context;
    private Socket socket;
    private String macToSend;

    FileTransferConnectTask(Context context,RecordData data,int flags){
        // flags=0 正常模式，flags=1 内网模式
        this.context=context;
        dialog=new ProgressDialog(context);
        this.data=data;

        // mac 地址格式： 无：，大写
        macToSend=data.getMac().replace(":","").toUpperCase();

        this.flags=flags;
    }
    private RecordData data;
    private ProgressDialog dialog;
    private String message= "";
    private int resultCode=-1;
    private String IP;
    private int flags;

    private String recvStr;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", this);
        dialog.setTitle("正在连接");
        Log.d("文件传输：","mac地址为："+data.getMac());
        dialog.setMessage("正在初始化参数");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        dialog.dismiss();
        if (resultCode!=-1){
            //跳转到文件Activity
            ToastMessageTool.tts(context,"连接成功");
            Intent intent=new Intent(context,FileTransferFolderActivity.class);
            intent.putExtra("detail",recvStr);
            intent.putExtra("mac",data.getMac());
            intent.putExtra("flags",flags);
            intent.putExtra("ip",IP);
            context.startActivity(intent);
        }
        else {
            new AlertDialog.Builder(context)
                    .setTitle("连接失败")
                    .setMessage(message)
                    .setNegativeButton("确定",null)
                    .show();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage(values[0]);
    }

    @Override
    protected Void doInBackground(RecordData... recordData) {
        if (data!=null){
            //检查可用性
            if (flags==0){
                // 判断是否是内网模式
                IP=PingAndConfirmTool.findCorrectIP(context,data);
                publishProgress(new String[]{"正在连接至："+data.getName()+"("+IP+")"});
            }
            else {
                IP=context.getString(R.string.nat_server);
                publishProgress(new String[]{context.getString(R.string.msg_connecting_nat)});
            }
            if (IP!=null){
                //尝试SSL连接目标IP
                try {
//                    socket=new Socket(IP,WLANDeviceData.unlock_port);
                    SocketHolder.setSocket(FileTransferActivity.CreateSocket(context,IP));
                    socket=SocketHolder.getSocket();
                    if (socket != null) {
//                        socket.setSoTimeout(3000);
                        OutputStream stream=socket.getOutputStream();
                        //发送根目录请求
                        JSONObject object=new JSONObject();
                        if (flags==1){
                            object.put("oriMac",macToSend);
                        }
                        object.put("action","Query") ;
                        object.put("path","/.");
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //

                        //读入数据
                        BufferedInputStream buffered = new BufferedInputStream(socket.getInputStream());
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int r=-1;
                        byte buff[] =new byte[1024];
                        while((r =buffered .read(buff,0,1024))!=-1)
                        {
                            byteArrayOutputStream.write(buff,0,r);
//                            if(buffered.available() <=0) //添加这里的判断
//                            {
//                                break;
//                            }
                        }
                        stream.close();
                        socket.close();
                        recvStr =new String(byteArrayOutputStream.toByteArray());

                        JsonObject object1=new Gson().fromJson(recvStr,JsonObject.class);
                        
                        if (object1==null){
                            // 空值也是离线
                            throw new IOException(context.getString(R.string.msg_device_offline));
                        }
                        
                        if (!object1.has("status")){
                            throw new IOException(context.getString(R.string.msg_no_responce_state));
                        }

                        if (object1.get("status").getAsString().equals("0")){
                            message=recvStr;
                            resultCode=0;
                        }
                        else {
                            switch (object1.get("status").getAsString()){
                                case "-1":
                                    throw new IOException(context.getString(R.string.msg_permission_error));
                                case "-2":
                                    throw new IOException(context.getString(R.string.msg_device_offline));
                                default:
                                    throw new IOException(context.getString(R.string.msg_unknown_error));
                            }
                        }
                    }
                } catch (IOException e) {
                    message=e.getMessage();
                } catch (JSONException e) {
                    message=e.getMessage();
                }
            }
        }
        return null;
    }

    private Runnable stop_r=new Runnable() {
        @Override
        public void run() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (SocketHolder.getSocket() != null && !SocketHolder.getSocket().isClosed()) {
                try {
                    SocketHolder.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Thread stopt=new Thread(stop_r);
        try {
            stopt.start();
            stopt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean result=this.cancel(true);
        if (result){
                ToastMessageTool.tts(context,"取消成功");
        }
    }
}
