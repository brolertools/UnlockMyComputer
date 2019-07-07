package com.kingtous.remotefingerunlock.FileTransferTool;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;

import moe.feng.support.biometricprompt.BiometricPromptCompat;

public class FileTransferActivity extends AppCompatActivity implements View.OnClickListener {

    Spinner spinner_devices;
//    Spinner spinner_mode;
    Button btn_ok;
    int device_selected_index=-1;
    int mode_selected_index=0; // 默认为正常模式
    SharedPreferences preferences;

    ArrayList<RecordData> list;
    ArrayList<String> list_show=new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_transfer_main);
        spinner_devices=findViewById(R.id.file_transfer_spinner_devices);
//        spinner_mode=findViewById(R.id.file_transfer_spinner_mode);
        btn_ok=findViewById(R.id.file_transfer_btn_ok);
        btn_ok.setOnClickListener(this);
        preferences= PreferenceManager.getDefaultSharedPreferences(this);
        mode_selected_index=Integer.valueOf(preferences.getString(getString(R.string.connect_mode),"0"));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.back2);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 读取当前存储的WiFi设备
        initWiFiDevices();
        initModes();
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(getResources().getColor(R.color.deepskyblue));
    }

    void initWiFiDevices(){
        //读取Database
        DataQueryHelper helper=new DataQueryHelper(this,getString(R.string.sqlDBName),null,1);
        list=RecordSQLTool.getAllWLANData(helper.getReadableDatabase());
        if (list!=null) {
            Log.d("WLAN数据库数量", String.valueOf(list.size()));
            //list -> list_show
            if (list.size()==0){
                Log.d("数据库WLAN条数","为空");
                btn_ok.setClickable(false);
                return;
            }
            for (RecordData data : list) {
                list_show.add(data.getName());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.file_transfer_spinner_item, R.id.file_transfer_item_textView, list_show);
            spinner_devices.setAdapter(arrayAdapter);
            spinner_devices.setOnItemSelectedListener(new DeviceSpinnerListener());

//            String[] modeStrArr=getResources().getStringArray(R.array.conn_modes);
//            ArrayAdapter<String> modeAdapter=new ArrayAdapter<>(this,R.layout.file_transfer_spinner_item,R.id.file_transfer_item_textView,modeStrArr);
//            spinner_mode.setAdapter(modeAdapter);
//            spinner_mode.setOnItemSelectedListener(new ModeSpinnerListener());
        }
        else {
            Log.e("数据库","异常");
        }
        helper.close();
    }

    void initModes(){


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.file_transfer_btn_ok:
                Log.d("DEBUG","ok clicked");
                if (device_selected_index!=-1 && device_selected_index<list.size()){
                    int flags=mode_selected_index;
                    BiometricPromptCompat promptCompat=FunctionTool.getAuthFingerPrompt(this);
                    promptCompat.authenticate(new CancellationSignal(), new BiometricPromptCompat.IAuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @Nullable CharSequence errString) {

                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, @Nullable CharSequence helpString) {

                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPromptCompat.IAuthenticationResult result) {
                            FileTransferConnectTask task=new FileTransferConnectTask(FileTransferActivity.this,list.get(device_selected_index),flags);
                            task.execute();
                        }

                        @Override
                        public void onAuthenticationFailed() {

                        }
                    });
                }
                else {
                    Log.e("ERROR","index错误");
                }
                break;
        }
    }

    class DeviceSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            device_selected_index=position;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            device_selected_index=-1;
        }
    }


    public static Socket CreateSocket(Context context,String IP){
        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
            try {
                if (preferences.getString(context.getString(R.string.connect_mode),"0").equals("0")) {
                    return SSLSecurityClient.CreateSocket(context, IP, WLANDeviceData.transfer_port);
                }
                else return SSLSecurityClient.CreateSocket(context, IP, WLANDeviceData.nat_transfer_port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
    }


}
