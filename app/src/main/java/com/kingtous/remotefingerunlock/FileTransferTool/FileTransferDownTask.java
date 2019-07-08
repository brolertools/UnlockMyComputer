package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;

import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

import static com.kingtous.remotefingerunlock.Common.FunctionTool.MIME_MapTable;

public class FileTransferDownTask extends AsyncTask<String, String, Void> implements DialogInterface.OnClickListener{



    private Context context;
    FileTransferDownTask(Context context, String IP, FileModel.DetailBean detailBean,String MAC){
        this.context=context;
        dialog=new ProgressDialog(context);
        this.IP=IP;
        this.MAC=MAC;
        this.detailBean= detailBean;
    }

    String message="";
    ProgressDialog dialog;
    private int resultCode=-1;
    String path;
    private String IP;
    private String MAC;
    String savePath;
    FileModel.DetailBean detailBean;
    private File tmpFile;


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", this);
        dialog.setTitle("正在下载");
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
                    .setMessage(message)
                    .setNegativeButton("确定", null)
                    .show();
        }
        else {
            new AlertDialog.Builder(context)
                    .setTitle("下载成功")
                    .setMessage("保存在:"+savePath+"\n请问接下来...")
                    .setPositiveButton("打开", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            processFile();
                        }
                    })
                    .setNeutralButton("发送", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendFiles();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage("共"+String.valueOf(((double)detailBean.getSize()/1024))+"KB"+"\n当前已下载"+values[0]);
//        dialog.setMessage("正在下载，请稍后");
    }

    @Override
    protected Void doInBackground(String... strings) {
            //检查可用性
            path =strings[0];
            if (path !=null){
                //尝试SSL连接目标IP
                try {
                    if (SocketHolder.getSocket().isClosed()) {
                            SocketHolder.setSocket(FileTransferActivity.CreateSocket(context, IP));
                    }
                    if (SocketHolder.getSocket() != null) {
                        OutputStream stream=SocketHolder.getSocket().getOutputStream();
                        //发送目录请求
                        JSONObject object=new JSONObject();
                        object.put("action","Get");
                        object.put("path",path);
                        object.put("mac",MAC);
                        if (FunctionTool.detectModes(context)==1){
                            object.put("oriMac",FunctionTool.macAddressAdjust(MAC));
                        }
                        stream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                        //
//                        stream.close();
                        //读入数据
                        BufferedInputStream buffered = new BufferedInputStream(SocketHolder.getSocket().getInputStream());
                        int r=-1;
                        byte buff[] =new byte[1024];
                        savePath = Environment.getExternalStorageDirectory().getPath() + "/Download/" + detailBean.getFile_name();
                        FileOutputStream file = new FileOutputStream(savePath, false);

                        long downSize=0;
                        long fileSize=detailBean.getSize();

                        //百分数
                        NumberFormat nf = NumberFormat.getPercentInstance();
                        nf.setMaximumFractionDigits(2);

                        while((r=buffered.read(buff,0,1024))!=-1)
                        {
                            downSize=downSize+r;
                            double num=((double) downSize/fileSize);
                            publishProgress(nf.format(num));
                            file.write(buff,0,r);
//                            if(buffered.available() <=0) //添加这里的判断
//                            {
//                                break;
//                            }
                        }
                        file.close();
                        if (downSize!=fileSize){
                            // 说明不是文件，看看是不是json数据
                            File file1=new File(savePath);
                            tmpFile=file1;
                            if (file1.exists()){
                                FileReader reader=new FileReader(file1);
                                BufferedReader br=new BufferedReader(reader);
                                String cmd=br.readLine();
                                try {
                                    JsonObject object1=new Gson().fromJson(cmd,JsonObject.class);

                                    if (!object1.has("status")){
                                        throw new IOException(context.getString(R.string.msg_no_responce_state));
                                    }

                                    switch (object1.get("status").getAsString()){
                                        case "-1":
                                            throw new IOException(context.getString(R.string.msg_permission_error));
                                        default:
                                            throw new IOException(context.getString(R.string.msg_unknown_error));
                                    }
                                } catch (JsonSyntaxException e){
                                    //不是
                                    throw  new IOException(context.getString(R.string.msg_invalid_data));
                                }catch (NullPointerException e){
                                    // 没有发数据
                                    throw new IOException(context.getString(R.string.msg_no_responce_data));
                                }
                                finally {
                                    br.close();
                                    reader.close();
                                    file1.deleteOnExit();
                                }
                            }
                        }

                        resultCode=0;
                    }
                } catch (IOException e) {
                    message=e.getMessage();
                } catch (JSONException e) {
                    message=e.getMessage();
                }
                finally {
                    try {
                        if (!SocketHolder.getSocket().isClosed())
                            SocketHolder.getSocket().close();
                    } catch (IOException e) {

                    }
                }
            }
        return null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // 关闭socket

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketHolder.getSocket().close();
                } catch (IOException ignored) {

                }
            }
        }).start();
        this.cancel(true);

    }

    private void sendFiles(){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_SEND);
        // 获得拓展名
        String type=FunctionTool.getFormatNameOfFile(savePath);
        File file=new File(savePath);
        Uri contentUri= FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",file);
        intent.putExtra(Intent.EXTRA_STREAM,contentUri);
        intent.setType(type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e){
            ToastMessageTool.tts(context,"没有能打开此类型的应用");
        }
    }

    private void processFile(){
        Intent intent=new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        // 获得拓展名
        String type=FunctionTool.getFormatNameOfFile(detailBean.getFile_name());
        File file=new File(savePath);
        Uri contentUri= FileProvider.getUriForFile(context,context.getPackageName()+".fileprovider",file);
        intent.setDataAndType(contentUri,type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e){
            ToastMessageTool.tts(context,"没有能打开此类型的应用");
        }
    }

}
