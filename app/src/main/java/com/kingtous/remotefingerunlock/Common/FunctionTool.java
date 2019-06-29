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

    public static final String directMode="0";
    public static final String remoteMode="1";

    public static int detectModes(Context context){
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getString(context.getString(R.string.connect_mode),"0").equals("0")) {
            return Integer.valueOf(directMode);
        }
        else return Integer.valueOf(remoteMode);
    }

    public static void editModes(Context context,int mode){
        String modeStr=String.valueOf(mode);
        if (modeStr.equals(directMode) || modeStr.equals(remoteMode)){
            SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor=preferences.edit();
            editor.putString(context.getString(R.string.connect_mode),String.valueOf(mode));
            editor.apply();
        }
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
                                    FunctionTool.showAlert(context,context.getString(R.string.msg_shutdown_success));
                                }
                                else {
                                    FunctionTool.showAlert(context,message);
                                }
                            }
                        });
                        transferShutDownTask.execute();
                    }
                })
                .setNegativeButton("取消",null)
                .show();
    }


    public static String macAddressAdjust(String macAddress){
        return macAddress.replace(":","").toUpperCase();
    }


    public static void showAlert(Context context,String message){
        new AlertDialog.Builder(context).setMessage(message).setPositiveButton("确定",null).show();
    }


}
