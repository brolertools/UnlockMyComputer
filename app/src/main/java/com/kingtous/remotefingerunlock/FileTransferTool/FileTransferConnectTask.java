package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Trace;

import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.PingAndConfirmTool;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class FileTransferConnectTask extends AsyncTask<RecordData, String, Void> implements DialogInterface.OnClickListener{

    private Context context;

    FileTransferConnectTask(Context context,RecordData data){
        this.context=context;
        dialog=new ProgressDialog(context);
        this.data=data;
    }
    RecordData data;
    ProgressDialog dialog;
    String message= "";
    int resultCode=-1;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", this);
        dialog.setTitle("正在连接");
        dialog.setMessage("正在初始化参数");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        dialog.dismiss();
        if (resultCode!=-1){
            //跳转到文件Activity
            Intent intent=new Intent(context,FileTransferFolderActivity.class);
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
            String IP=PingAndConfirmTool.findCorrectIP(context,data);
            publishProgress(new String[]{"正在连接至："+data.getName()+"("+IP+")"});
            if (IP!=null){
                //尝试SSL连接目标IP
                try {
                    Socket socket=SSLSecurityClient.CreateSocket(context,IP, WLANDeviceData.port);
                    if (socket != null) {
                        OutputStream stream=socket.getOutputStream();
                        //TODO 验证

                        //发送根目录请求
                        JSONObject object=new JSONObject();
                        object.put("Query","/");
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //
                        stream.close();

                        //读入数据
                        BufferedInputStream buffered = new BufferedInputStream(socket.getInputStream());
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int r=-1;
                        byte buff[] =new byte[1024];
                        while((r =buffered .read(buff,0,1024))!=-1)
                        {
                            byteArrayOutputStream.write(buff,0,r);
                            if(buffered .available() <=0) //添加这里的判断
                            {
                                break;
                            }

                        }
                        String re =new  String(byteArrayOutputStream.toByteArray());
                        message=re;
                        resultCode=0;
                        socket.close();
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


    @Override
    public void onClick(DialogInterface dialog, int which) {
        this.cancel(true);
    }
}
