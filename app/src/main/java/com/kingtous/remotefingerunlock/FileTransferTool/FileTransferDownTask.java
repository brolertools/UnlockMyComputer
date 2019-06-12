package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;

import com.google.gson.Gson;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileTransferDownTask extends AsyncTask<String, String, Void> implements DialogInterface.OnClickListener{



    private Context context;
    FileTransferDownTask(Context context, String IP, FileModel.DetailBean detailBean){
        this.context=context;
        dialog=new ProgressDialog(context);
        this.IP=IP;
        this.detailBean= detailBean;
    }

    String message="";
    ProgressDialog dialog;
    private int resultCode=-1;
    String path;
    private String IP;
    String savePath;
    FileModel.DetailBean detailBean;


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", this);
        dialog.setMessage("正在请求下载");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        dialog.dismiss();
        if (resultCode==-1) {
            new AlertDialog.Builder(context)
                    .setTitle("下载失败")
                    .setNegativeButton("确定", null)
                    .show();
        }
        else {
            new AlertDialog.Builder(context)
                    .setTitle("下载成功")
                    .setMessage("保存在:"+savePath+"\n请问是否要打开？")
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                                builder.detectAll();
                                StrictMode.setVmPolicy(builder.build());
                            }
                            Intent intent=new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(savePath)),"*/*");
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton("否",null)
                    .show();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
//        dialog.setMessage("共"+String.valueOf(((double)detailBean.getSize()/1024)/1024)+"MB"+"\n当前已下载"+values[0]+"%");
        dialog.setMessage("正在下载，请稍后");
    }

    @Override
    protected Void doInBackground(String... strings) {
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
                        object.put("Down",path);
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //
                        stream.close();
                        //读入数据
                        BufferedInputStream buffered = new BufferedInputStream(SocketHolder.getSocket().getInputStream());
                        int r=-1;
                        byte buff[] =new byte[1024];
                        savePath = Environment.getExternalStorageDirectory().getPath() + "/" + detailBean.getFile_name();
                        FileOutputStream file = new FileOutputStream(savePath, false);

                        int downSize=0;

                        while((r=buffered.read(buff,0,1024))!=-1)
                        {
                            publishProgress();
//                            downSize=downSize+r;
//                            publishProgress(String.valueOf(((double) downSize /detailBean.getSize())*100));
                            file.write(buff,0,r);
                            if (downSize==detailBean.getSize()){
                                break;
                            }
//                            if(buffered.available() <=0) //添加这里的判断
//                            {
//                                break;
//                            }
                        }
                        file.close();
                        SocketHolder.getSocket().close();
                        resultCode=0;
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
