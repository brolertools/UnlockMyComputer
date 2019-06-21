package com.kingtous.remotefingerunlock.Common;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kingtous.remotefingerunlock.FileTransferTool.FileTransferShutDownTask;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import java.io.IOException;

import androidx.appcompat.app.AlertDialog;

public class FunctionTool {

    public static int detectModes(Context context){
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getString(context.getString(R.string.connect_mode),"0").equals("0")) {
            return 0;
        }
        else return 1;
    }


    public static void shutdown(final Context context, final String IP, final String MAC, final int flags){
        new AlertDialog.Builder(context)
                .setTitle("警告")
                .setMessage("关闭设备？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (MAC==null || MAC.equals("")){
                            dialog.dismiss();
                            new AlertDialog.Builder(context)
                                    .setMessage("记录无MAC地址")
                                    .setPositiveButton("确定",null)
                                    .show();
                            return;
                        }

                        String _IP=IP;
                        if (flags==1){
                            _IP=context.getString(R.string.nat_server);
                        }
                        FileTransferShutDownTask transferShutDownTask=
                                new FileTransferShutDownTask(context,_IP,MAC);
                        transferShutDownTask.setmReturnListener(new FileTransferShutDownTask.ReturnListener() {
                            @Override
                            public void onReturnListener(int resultCode, String message) {
                                if (resultCode==0){
                                    ToastMessageTool.tts(context,"关机指令发送成功");
                                }
                                else {
                                    ToastMessageTool.tts(context,message);
                                }
                            }
                        });
                        transferShutDownTask.execute();
                    }
                })
                .setNegativeButton("取消",null)
                .show();
    }


}
