package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileTransferQueryTask extends AsyncTask<String, String, FileModel> implements DialogInterface.OnClickListener{

    private Context context;
    FileTransferQueryTask(Context context, String IP){
        this.context=context;
        dialog=new ProgressDialog(context);
        this.IP=IP;
    }
    String message="";
    ProgressDialog dialog;
    private int resultCode=-1;
    String path;
    private String IP;
    private String recvStr;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", this);
        dialog.setMessage("正在请求");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected void onPostExecute(FileModel aModel) {
        dialog.dismiss();
        if (resultCode==-1) {
            new AlertDialog.Builder(context)
                    .setTitle("获取目标文件夹失败")
                    .setNegativeButton("确定", null)
                    .show();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage(values[0]);
    }

    @Override
    protected FileModel doInBackground(String... strings) {
            //检查可用性
            path =strings[0];
            if (path !=null){
                //尝试SSL连接目标IP
                try {
                    if (SocketHolder.getSocket().isClosed())
                        SocketHolder.setSocket(SSLSecurityClient.CreateSocket(context, IP, WLANDeviceData.port));
                    if (SocketHolder.getSocket() != null) {
                        OutputStream stream=SocketHolder.getSocket().getOutputStream();
                        //发送目录请求
                        JSONObject object=new JSONObject();
                        object.put("Query",path);
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //
                        stream.close();

                        //读入数据
                        BufferedInputStream buffered = new BufferedInputStream(SocketHolder.getSocket().getInputStream());
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
                        recvStr =new String(byteArrayOutputStream.toByteArray());
                        message=recvStr;
                        resultCode=0;
                        return new Gson().fromJson(recvStr,FileModel.class);
                    }
                } catch (IOException e) {
                    message=e.getMessage();
                } catch (JSONException e) {
                    message=e.getMessage();
                }
            }
        return null;
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        this.cancel(true);
    }
}
