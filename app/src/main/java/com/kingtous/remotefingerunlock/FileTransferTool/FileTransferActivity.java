package com.kingtous.remotefingerunlock.FileTransferTool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FileTransferActivity extends AppCompatActivity implements View.OnClickListener {

    Spinner spinner_devices;
    Button btn_ok;
    int device_selected_index=-1;

    ArrayList<RecordData> list;
    ArrayList<String> list_show=new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_transfer_main);
        spinner_devices=findViewById(R.id.file_transfer_spinner_devices);
        btn_ok=findViewById(R.id.file_transfer_btn_ok);
        btn_ok.setOnClickListener(this);
        // 读取当前存储的WiFi设备
        initWiFiDevices();
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
        }
        else {
            Log.e("数据库","异常");
        }
        helper.close();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.file_transfer_btn_ok:
                Log.d("DEBUG","ok clicked");

                if (device_selected_index!=-1 && device_selected_index<list.size()){
                    FileTransferConnectTask task=new FileTransferConnectTask(this,list.get(device_selected_index));
                    task.execute();
                }
                else {
                    Log.e("ERROR","index错误了");
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

}
