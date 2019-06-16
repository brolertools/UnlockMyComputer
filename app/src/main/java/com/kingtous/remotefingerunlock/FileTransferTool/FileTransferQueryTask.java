package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
                    .setMessage(message)
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
                        SocketHolder.setSocket(SSLSecurityClient.CreateSocket(context, IP, WLANDeviceData.transfer_port));
//                    SocketHolder.setSocket(new Socket(IP,2090));
                    if (SocketHolder.getSocket() != null) {
                        OutputStream stream=SocketHolder.getSocket().getOutputStream();
                        //发送目录请求
                        JSONObject object=new JSONObject();
                        object.put("action","Query");
                        object.put("path",path);
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //

//                      stream.close();
                        //读入数据
//                        SocketHolder.getSocket().setSoTimeout(5000);
                        BufferedInputStream buffered = new BufferedInputStream(SocketHolder.getSocket().getInputStream());
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int r=-1;
                        byte buff[] =new byte[1024];
                        while((r=buffered.read(buff,0,1024))!=-1)
                        {
                            byteArrayOutputStream.write(buff,0,r);
//                            if(buffered.available() <=0) //添加这里的判断
//                            {
//                                break;
//                            }
                        }
                        SocketHolder.getSocket().close();
                        recvStr =new String(byteArrayOutputStream.toByteArray());
                        JsonObject object1=new Gson().fromJson(recvStr,JsonObject.class);

                        message=recvStr;
                        resultCode=0;

                        return new Gson().fromJson(object1,FileModel.class);
//                        if (!object1.has("status")){
//                            throw new IOException("未返回状态码");
//                        }
//
//                        if (object1.get("status").getAsString().equals("0")){
//                            message=recvStr;
//                            resultCode=0;
//
//                            return new Gson().fromJson(object1,FileModel.class);
//                        }
//                        else {
//                            switch (object1.get("status").getAsString()){
//                                case "-1":
//                                    throw new IOException("权限错误");
//                                default:
//                                    throw new IOException("未知错误");
//                            }
//                        }

                    }
                } catch (IOException e) {
                    message=e.getMessage();
                } catch (JSONException e) {
                    message=e.getMessage();
                }
                finally {
                    if (recvStr!=null && !recvStr.equals("")){
                        message=message+"\n收到以下内容：\n"+recvStr;
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
