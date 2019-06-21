package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.PingAndConfirmTool;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

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

    FileTransferConnectTask(Context context,RecordData data,int flags){
        // flags=0 正常模式，flags=1 内网模式
        this.context=context;
        dialog=new ProgressDialog(context);
        this.data=data;
        this.flags=flags;
    }
    RecordData data;
    ProgressDialog dialog;
    String message= "";
    private int resultCode=-1;
    String IP;
    int flags;

    private String recvStr;

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
                publishProgress(new String[]{"正在连接至内网服务器，等待回应"});
            }
            if (IP!=null){
                //尝试SSL连接目标IP
                try {
                    socket=FileTransferActivity.CreateSocket(context,IP);
//                    socket=new Socket(IP,WLANDeviceData.unlock_port);
                    SocketHolder.setSocket(socket);
                    if (socket != null) {
                        OutputStream stream=socket.getOutputStream();
                        //TODO 验证

                        //发送根目录请求
                        JSONObject object=new JSONObject();
                        if (flags==1){
                            object.put("oriMac",data.getMac());
                            // 内网表明客户端身份
                            object.put("type","client");
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
                        message=recvStr;
                        resultCode=0;

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
