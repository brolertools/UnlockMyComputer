package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kingtous.remotefingerunlock.Common.RegexTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.Security.SecurityTransform;
import com.kingtous.remotefingerunlock.R;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.subnet.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import pub.devrel.easypermissions.EasyPermissions;

public class WLANConnectActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {


    static int Text=0;
    static int Info=1;
    // ping主机超时时间
    int timeout=200;
    // handler更新列表的标识码
    int updateList=0;
    int updateProgress=1;
//    int searchEnd=1;
    SearchTask task;


    WifiManager manager;
    ArrayList<WLANDeviceData> deviceDatalist=new ArrayList<>();
    RecyclerView lst_wlan;
    WLANRecyclerAdapter adapter;
    TextView title;//扫描时实时更新
    SwipeRefreshLayout refreshLayout;

    //Button
    Button btn_back;
    Button btn_auto;
    Button btn_manual;


    Runnable updateUi=new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
        }
    };
    Runnable searchEnd=new Runnable() {
        @Override
        public void run() {
            stopSearch();
        }
    };



    //权限
    int WIFI_REQUEST_CODE=2;
    String[] permission={
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
    };

    class SearchTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ToastMessageTool.tts(WLANConnectActivity.this,"正在搜索");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SubnetDevices.fromLocalAddress().findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                @Override
                public void onDeviceFound(Device device) {
                    WLANDeviceData data=new WLANDeviceData(device.hostname,device.mac,device.ip);
                    for (WLANDeviceData listData : deviceDatalist){
                        if (data.getMac()==null || listData.getMac()==null || listData.getMac().equals(data.getMac())){
                            return;
                        }
                    }
                    deviceDatalist.add(data);
                    publishProgress();
                }

                @Override
                public void onFinished(ArrayList<Device> arrayList) {

                }
            });
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ToastMessageTool.tts(WLANConnectActivity.this,"搜索完成");
        }
    }

    void startSearch(){
        deviceDatalist.clear();
        task=new SearchTask();
        task.execute();
    }

    void stopSearch(){

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list);
        title=findViewById(R.id.title_WLAN);
        refreshLayout=findViewById(R.id.lst_WLAN_swipe);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startSearch();
                refreshLayout.setRefreshing(false);
            }
        });
        //按钮监听
        btn_back=findViewById(R.id.btn_WLAN_back);
        btn_auto=findViewById(R.id.btn_WLAN_search);
        btn_manual=findViewById(R.id.btn_WLAN_manualInput);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btn_auto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
            }
        });
        btn_manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View view=LayoutInflater.from(WLANConnectActivity.this).inflate(R.layout.dialog_wifimanual,null,false);

                new AlertDialog.Builder(WLANConnectActivity.this)
                        .setView(view)
                        .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 手动添加
                                EditText User=view.findViewById(R.id.edit_WIFI_username);
                                EditText passwd=view.findViewById(R.id.edit_WIFI_passwd);
                                EditText name=view.findViewById(R.id.edit_WIFI_name);
                                EditText Ip=view.findViewById(R.id.edit_WIFI_address);
                                CheckBox box_store=view.findViewById(R.id.dialog_wifi_checkbox_storeConnection);
                                CheckBox box_default=view.findViewById(R.id.dialog_wifi_checkbox_setDefault);
                                Pattern pattern=Pattern.compile(RegexTool.ipRegex);
                                Matcher matcher=pattern.matcher(Ip.getText());
                                if (matcher.matches()){
                                    //mac地址不需要用户输入
                                    RecordData dataTmp=new RecordData("WLAN",
                                            name.getText().toString(),
                                            User.getText().toString(),
                                            passwd.getText().toString(),
                                            Ip.getText().toString(),
                                            null
                                    );
                                    // ping然后获取mac
                                    String s= ARPInfo.getMACFromIPAddress(Ip.getText().toString());
                                    if (s==null){
                                        Toast.makeText(WLANConnectActivity.this,"未获取到ip对应的mac地址",Toast.LENGTH_LONG).show();

                                    } else {
                                        dataTmp.setMac(s.toUpperCase());
                                    }
                                    if (box_default.isChecked()){
                                        dataTmp.setIsDefault(RecordData.TRUE);
                                    }
                                    if (box_store.isChecked()){
                                        SQLiteOpenHelper helper= new DataQueryHelper(WLANConnectActivity.this,
                                                getString(R.string.sqlDBName),
                                                null,
                                                1
                                                );
                                        RecordSQLTool.addtoSQL(helper,dataTmp);
                                    }
                                    startConnect(dataTmp);
                                }
                                else {
                                    new AlertDialog.Builder(WLANConnectActivity.this)
                                            .setMessage("输入IP有误，请核对")
                                            .setPositiveButton("确定",null)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton("取消",null)
                        .show();
            }
        });

        //设置服务和控件
        lst_wlan=findViewById(R.id.lst_WLAN);
        manager=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        adapter=new WLANRecyclerAdapter(deviceDatalist);
        adapter.setOnItemClickListener(new WLANRecyclerAdapter.OnItemClickListener() {
            @Override
            public void OnClick(final View view, final int Position) {
                final View view1=LayoutInflater.from(WLANConnectActivity.this).inflate(R.layout.dialog_user_passwd,null,false);
                final EditText name=view1.findViewById(R.id.edit_name);
                final EditText User=view1.findViewById(R.id.edit_username);
                final EditText passwd=view1.findViewById(R.id.edit_passwd);
                final TextView Ip=view.findViewById(R.id.name_WLAN_device_ip);
                final TextView mac=view.findViewById(R.id.name_WLAN_device_mac);
                final CheckBox box_store=view1.findViewById(R.id.dialog_checkbox_storeConnection);
                final CheckBox box_default=view1.findViewById(R.id.dialog_checkbox_setDefault);
                Pattern pattern=Pattern.compile(RegexTool.ipRegex);
                final Matcher matcher=pattern.matcher(Ip.getText().toString());
                name.setText(deviceDatalist.get(Position).getName());

                new AlertDialog.Builder(WLANConnectActivity.this)
                        .setView(view1)
                        .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (matcher.matches()){

                                    RecordData dataTmp=new RecordData("WLAN",
                                            name.getText().toString(),
                                            User.getText().toString(),
                                            passwd.getText().toString(),
                                            Ip.getText().toString(),
                                            mac.getText().toString().toUpperCase()
                                    );
                                    if (box_default.isChecked()){
                                        dataTmp.setIsDefault(RecordData.TRUE);
                                    }
                                    if (box_store.isChecked()){
                                        SQLiteOpenHelper helper= new DataQueryHelper(WLANConnectActivity.this,
                                                getString(R.string.sqlDBName),
                                                null,
                                                1
                                        );
                                        RecordSQLTool.addtoSQL(helper,dataTmp);
                                    }
                                    startConnect(dataTmp);
                                }
                                else {
                                    new AlertDialog.Builder(WLANConnectActivity.this)
                                            .setMessage("输入IP有误，请核对")
                                            .setPositiveButton("确定",null)
                                            .show();
                                }

                            }
                        })
                        .setNegativeButton("取消",null)
                        .show();
            }
        });

        //Wifi三连
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        lst_wlan.setLayoutManager(linearLayoutManager);
        lst_wlan.setAdapter(adapter);

        //广播
        IntentFilter filter=new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver,filter);
        checkWLAN(WLANConnectActivity.this,manager);
    }

    BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            int extra;
            assert action != null;
            switch (action){
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    extra=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,-1);
                    switch (extra){
                        case WifiManager.WIFI_STATE_ENABLED:
                            //检测wifi是否连接无线局域网
                            if (isWifiConnected()){
                                //wifi已开启，开始向局域网广播
                                startSearch();
                            }
                            else {
                                Toast.makeText(WLANConnectActivity.this,"WLAN还未连接",Toast.LENGTH_LONG).show();
                            }
                            break;

                        case WifiManager.WIFI_STATE_DISABLING:
                            log("Wifi关闭,停止扫描");
                                stopSearch();
                            break;
                        default:
                            break;
                    }
                    break;

                case ConnectivityManager.CONNECTIVITY_ACTION:
                    extra=intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE,-1);
                    switch (extra)
                    {
                        case ConnectivityManager.TYPE_WIFI:
                            //wifi已开启，开始向局域网广播
                            startSearch();
                            break;
                        default:
                            break;
                    }
                    break;
                    default:
                    break;
            }
        }
    };

    private boolean isWifiConnected() {
        /*
        判断是否为Wifi连接
         */
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        int state=networkInfo.getType();
        switch (state) {
            case ConnectivityManager.TYPE_WIFI:
                return true;
            default:
                return false;
        }


    }


    private void checkPermission(Context context){
        if (!EasyPermissions.hasPermissions(context,permission)){
            EasyPermissions.requestPermissions(this,"此功能需要WLAN相关权限",WIFI_REQUEST_CODE,permission);
        }
        return;
    }


    public void checkWLAN(final Context context, final WifiManager manager){
        checkPermission(context);
        if (manager!=null && !manager.isWifiEnabled()){

            final NiftyDialogBuilder builder=NiftyDialogBuilder.getInstance(WLANConnectActivity.this);


            builder.withEffect(Effectstype.Fall)
                    .withDialogColor(R.color.deepskyblue)
                    .withTitle("WLAN检测")
                    .withMessage("未打开WLAN，请问是否开启?")
                    .isCancelableOnTouchOutside(false)
                    .withButton1Text("打开")
                    .withButton2Text("取消")
                    .setButton1Click(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            manager.setWifiEnabled(true);
                            builder.dismiss();
                        }
                    })
                    .setButton2Click(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    })
                    .show();
        }
        else return;

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode==WIFI_REQUEST_CODE){
            finish();
        }
    }



    void startConnect(RecordData data){
        WLANConnect connect=new WLANConnect(this,data);
        connect.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void log(String text){
        Toast.makeText(WLANConnectActivity.this,text,Toast.LENGTH_LONG).show();
    }

}
