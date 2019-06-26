package com.kingtous.remotefingerunlock.ToolFragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kingtous.remotefingerunlock.BluetoothConnectTool.BluetoothConnectActivity;
import com.kingtous.remotefingerunlock.Common.QRCodeScannerActivity;
import com.kingtous.remotefingerunlock.Common.RegexTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANConnectActivity;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.Ping;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import pub.devrel.easypermissions.EasyPermissions;

import static com.kingtous.remotefingerunlock.ToolFragment.DataManagementFragment.iPpattern;
import static com.kingtous.remotefingerunlock.ToolFragment.DataManagementFragment.maCpattern;

public class ScanFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    public ScanFragment() {

    }

    private Button btn_WL;
    private Button btn_ML;
    private Button btn_BT;
    private Button btn_QR;


    public static int BT_RequestCode = 1;
    public static int WL_RequestCode = 2;
    public static int QR_RequestCode = 3;

    String[] permissions=new String[]{Manifest.permission.CAMERA};
    int CAMERA_GRANT=1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scan_list, container, false);
        btn_WL = view.findViewById(R.id.btn_WLAN);
        btn_BT = view.findViewById(R.id.btn_BLUETOOTH);
        btn_ML = view.findViewById(R.id.btn_MANUAL);
        btn_QR = view.findViewById(R.id.btn_qrcode);

        btn_WL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), WLANConnectActivity.class);
                startActivityForResult(intent, WL_RequestCode);
            }
        });


        btn_BT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), BluetoothConnectActivity.class);
                startActivityForResult(intent, BT_RequestCode);
            }
        });

        btn_ML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View diaView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_add, null, false);
                add(getContext(),diaView);
            }
        });

        btn_QR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //权限申请
                if (EasyPermissions.hasPermissions(Objects.requireNonNull(getContext()),permissions)){
                    Intent intent = new Intent(getContext(), QRCodeScannerActivity.class);
                    startActivityForResult(intent, QR_RequestCode);
                }
                else {
                    EasyPermissions.requestPermissions(Objects.requireNonNull(getActivity()),"申请使用相机",CAMERA_GRANT,permissions);
                }
            }
        });

        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_RequestCode) {

        } else if (requestCode == WL_RequestCode) {
            View diaView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_add, null, false);
            add(getContext(),diaView);
        }
        else if (requestCode==QR_RequestCode){
            if (resultCode==QRCodeScannerActivity.OK){
                View diaView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_add, null, false);
                RadioButton wific=diaView.findViewById(R.id.manual_type_wlan);
                EditText name=diaView.findViewById(R.id.manual_name_edit);
                EditText ip=diaView.findViewById(R.id.manual_ip_edit);
                EditText mac=diaView.findViewById(R.id.manual_mac_edit);
                JsonObject object=new Gson().fromJson(data.getStringExtra("result"),JsonObject.class);
                wific.setChecked(true);
                name.setText(object.get("hostname").getAsString());
                ip.setText(object.get("ip").getAsString());
                mac.setText(object.get("mac").getAsString());
                add(getContext(),diaView);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    private boolean addRecord(String Type, String name, String user, String passwd, String ip, String mac, int setDefault) {
        boolean result = false;
        if (user.equals("") || passwd.equals("") || (mac.equals("") && ip.equals(""))) {
            Toast.makeText(getContext(), getString(R.string.input_illegal), Toast.LENGTH_SHORT).show();
        } else {
            DataQueryHelper helper = new DataQueryHelper(getContext(), getString(R.string.sqlDBName), null, 1);
            RecordData data = new RecordData(Type, name, user, passwd, ip, mac, setDefault);
            if (RecordSQLTool.addtoSQL(helper, data)) {
                Toast.makeText(getContext(), getString(R.string.store_success), Toast.LENGTH_LONG).show();
                result = true;
            } else
                Toast.makeText(getContext(), getString(R.string.store_failed_due_same_mac_user), Toast.LENGTH_LONG).show();
            helper.close();
        }
        return result;
    }


    public void add(final Context context, final View diaView){
        RadioButton btn_bl=diaView.findViewById(R.id.manual_type_bluetooth);//ban IP
        final EditText ip_edit=diaView.findViewById(R.id.manual_ip_edit);
        btn_bl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ip_edit.setText("");
                    ip_edit.setEnabled(false);
                }
                else ip_edit.setEnabled(true);
            }
        });
        final RadioGroup radioGroup = diaView.findViewById(R.id.manual_type_selected);
        new AlertDialog.Builder(context)
                .setView(diaView)
                .setPositiveButton("添加", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int id = radioGroup.getCheckedRadioButtonId();
                        String type;
                        if (id == R.id.manual_type_wlan)
                            type = "WLAN";
                        else if (id == R.id.manual_type_bluetooth)
                            type = "Bluetooth";
                        else {
                            Toast.makeText(context, context.getString(R.string.warn_select_connect), Toast.LENGTH_LONG).show();
                            return;
                        }
                        final EditText name = diaView.findViewById(R.id.manual_name_edit);
                        final EditText ip = diaView.findViewById(R.id.manual_ip_edit);
                        final EditText mac = diaView.findViewById(R.id.manual_mac_edit);
                        Matcher matcher1 = maCpattern.matcher(mac.getText().toString().toUpperCase());
                        Matcher matcher2 = iPpattern.matcher(ip.getText().toString().toUpperCase());
                        final EditText user = diaView.findViewById(R.id.manual_user_edit);
                        final EditText passwd = diaView.findViewById(R.id.manual_passwd_edit);
                        CheckBox checkBox = diaView.findViewById(R.id.manual_setDefault);
                        //先测试

                        if (!matcher1.matches() && !matcher2.matches()) {
                            Toast.makeText(context, context.getString(R.string.address_illegal), Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (type.equals("WLAN")) {
                            String s= null;
                            Ping p = Ping.onAddress(ip.getText().toString());
                            p.setTimeOutMillis(500);
                            try {
                                p.doPing();
                            } catch (UnknownHostException e) {
                                ToastMessageTool.tts(context, e.getMessage());
                            }
                            if (! RegexTool.isStdMac(mac.getText().toString())){
                                s = ARPInfo.getMACFromIPAddress(ip.getText().toString());

                                if (s == null) {
                                    Toast.makeText(context,"未获取到ip对应的mac地址",Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(context, "自动获取到ip对应的mac地址\n" + s.toUpperCase(), Toast.LENGTH_LONG).show();
                                    mac.setText(s.toUpperCase());
                                }
                            }
                        }

                        if (checkBox.isChecked()) {
                            addRecord(type,
                                    name.getText().toString(),
                                    user.getText().toString(),
                                    passwd.getText().toString(),
                                    ip.getText().toString(),
                                    mac.getText().toString().toUpperCase(), RecordData.TRUE);
                        } else addRecord(type,
                                name.getText().toString(),
                                user.getText().toString(),
                                passwd.getText().toString(),
                                ip.getText().toString(),
                                mac.getText().toString().toUpperCase(), RecordData.FALSE);
                    }
                }).setNegativeButton("取消",null)
                .show();
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode==CAMERA_GRANT){
            Intent intent = new Intent(getContext(), QRCodeScannerActivity.class);
            startActivityForResult(intent, QR_RequestCode);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode==CAMERA_GRANT){
            Intent intent = new Intent(getContext(), QRCodeScannerActivity.class);
            startActivityForResult(intent, QR_RequestCode);
        }
        else
        {
            EasyPermissions.requestPermissions(Objects.requireNonNull(getActivity()),"申请使用相机",CAMERA_GRANT,permissions);
        }
    }
}
