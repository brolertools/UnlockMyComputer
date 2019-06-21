package com.kingtous.remotefingerunlock.ToolFragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kingtous.remotefingerunlock.Common.Connect;
import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.RegexTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.DataStoreTool.DataQueryHelper;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordData;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordAdapter;
import com.kingtous.remotefingerunlock.DataStoreTool.RecordSQLTool;
import com.kingtous.remotefingerunlock.MenuTool.RecordPopupMenuTool;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Widget.UnlockWidget;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.Ping;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import jp.wasabeef.recyclerview.animators.FlipInTopXAnimator;
import pub.devrel.easypermissions.EasyPermissions;

public class DataManagementFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    int WRITE_PERMISSION = 2;


    static Pattern maCpattern = Pattern.compile(RegexTool.macRegex);
    static Pattern iPpattern = Pattern.compile(RegexTool.ipRegex);


    private ArrayList<RecordData> recordDataArrayList;

    private RecyclerView data_view;
    private RecordAdapter adapter;

    private DataQueryHelper helper;
    private RelativeLayout app_empty;

    private SQLiteDatabase readSQL;
    private SQLiteDatabase writeSQL;

    public DataManagementFragment() {

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPermission();
        getRecord();
    }

    private void checkPermission() {
        String[] WritePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(Objects.requireNonNull(getContext()), WritePermission)) {
            EasyPermissions.requestPermissions(this, "存储数据需要写入权限", WRITE_PERMISSION, WritePermission);
        }
    }

    private void update() {
        if (recordDataArrayList.size() == 0) {
            app_empty.setVisibility(View.VISIBLE);
        } else app_empty.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        UnlockWidget.update(getActivity().getApplicationContext());
    }

    private void deleteRecord(RecordData recordData) {
        if (writeSQL != null) {
            try {
                //删除SQL中的元素
                if (recordData.getMac()!=null)
                    writeSQL.execSQL("delete from data where Mac='" + recordData.getMac() + "' and User='" + recordData.getUser() + "'" );
                else writeSQL.execSQL("delete from data where User='" + recordData.getUser() + "'" + " and Ip='" + recordData.getIp() + "'");
                //删除List中的
                this.recordDataArrayList.remove(recordData);
            } catch (Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean updateRecord(RecordData recordData, RecordData newlyRecordData) {
        //仅更新数据库
        boolean result = false;
        if (writeSQL != null) {
            try {
                RecordSQLTool.updatetoSQL(helper.getWritableDatabase(), recordData, newlyRecordData);
                if (newlyRecordData.getIsDefault() == RecordData.TRUE) {
                    result = RecordSQLTool.updateDefaultRecord(helper, newlyRecordData.getMac(), newlyRecordData.getUser(),newlyRecordData.getIp());
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return result;
    }

    private void getRecord() {
        if (readSQL != null) {
            Cursor cursor = readSQL.query
                    ("data", null, null, null, null, null, "Name");
            while (cursor.moveToNext()) {
                RecordData recordData = RecordSQLTool.toRecordData(cursor);
                this.recordDataArrayList.add(recordData);
            }
            update();
            cursor.close();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.data_management, container, false);
        recordDataArrayList = new ArrayList<>();
        data_view = view.findViewById(R.id.data_list);

        app_empty = view.findViewById(R.id.app_empty);
        app_empty.setVisibility(View.GONE);
        helper = new DataQueryHelper(getContext(), getString(R.string.sqlDBName), null, 1);
        readSQL = helper.getReadableDatabase();
        writeSQL = helper.getWritableDatabase();

        //动画
        data_view.setItemAnimator(new FlipInTopXAnimator());

        //LayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        data_view.setLayoutManager(layoutManager);

        //适配器
        adapter = new RecordAdapter(recordDataArrayList);
        adapter.setOnItemClickListener(new RecordAdapter.OnItemClickListener() {

            @Override
            public void OnClick(View view, int type, final RecordData recordData) {
                if (type == RecordAdapter.BOOT_BUTTON) {
                    //开机
                    boot(getContext(),recordData);
                } else if (type == RecordAdapter.UNLOCK_BUTTON) {
                    //连接
                    Toast.makeText(getContext(), "正在连接", Toast.LENGTH_LONG).show();
                    Connect.start(getContext(), recordData);
                }
                else if (type==RecordAdapter.SHUTDOWN_BUTTON){
                    FunctionTool.shutdown(getContext(),recordData.getIp(),recordData.getMac(),FunctionTool.detectModes(getContext()));
                }
                else if (type == RecordAdapter.MORE_BUTTON) {
                    //更多
                    PopupMenu menu = RecordPopupMenuTool.createInstance(Objects.requireNonNull(getContext()), view);
                    menu.show();
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.record_edit:
                                    edit(getContext(),recordData);
                                    break;
                                case R.id.record_delete:
                                    delete(getContext(),recordData);
                                    break;
                                default:
                                    return false;
                            }
                            return true;
                        }
                    });
                }

            }
        });
        data_view.setAdapter(adapter);
        return view;
    }


    public void boot(Context context, final RecordData recordData){
        if (recordData.getMac().equals("")) {
            new AlertDialog.Builder(getContext()).setMessage("记录无MAC地址").setPositiveButton("确定", null).show();
        }
        ToastMessageTool.tts(getContext(), "正在发送");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                RecordPopupMenuTool.wakeOnLAN(getContext(), recordData);
                Looper.loop();
            }
        }).start();
    }



    public void delete(Context context, final RecordData recordData){
        new AlertDialog.Builder(context)
                .setMessage("是否要删除所选记录?")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //删除
                        deleteRecord(recordData);
                        update();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void edit(Context context, final RecordData recordData){
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_add, null, false);

        final RadioGroup group = view1.findViewById(R.id.manual_type_selected);
//                    final RadioButton btn_wl =view1.findViewById(R.id.manual_type_wlan);
        final RadioButton btn_bt =view1.findViewById(R.id.manual_type_bluetooth);
        final EditText nameEdit = view1.findViewById(R.id.manual_name_edit);
        final EditText ipEdit = view1.findViewById(R.id.manual_ip_edit);
        final EditText macEdit = view1.findViewById(R.id.manual_mac_edit);
        final EditText userEdit = view1.findViewById(R.id.manual_user_edit);
        final EditText passwdEdit = view1.findViewById(R.id.manual_passwd_edit);
        final CheckBox checkBox = view1.findViewById(R.id.manual_setDefault);

        btn_bt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    // 蓝牙没有IP
                    ipEdit.setText("");
                    ipEdit.setEnabled(false);
                }
                else ipEdit.setEnabled(true);
            }
        });
        //填入数据
        //Type
        if (recordData.getType().equals("Bluetooth")) {
            ((RadioButton) view1.findViewById(R.id.manual_type_bluetooth)).setChecked(true);
        } else
            ((RadioButton) view1.findViewById(R.id.manual_type_wlan)).setChecked(true);
        ipEdit.setText(recordData.getIp());
        nameEdit.setText(recordData.getName());
        macEdit.setText(recordData.getMac());
        userEdit.setText(recordData.getUser());
        passwdEdit.setText(recordData.getPasswd());
        if (recordData.getIsDefault() == RecordData.TRUE) {
            //是默认的指纹设置
            checkBox.setChecked(true);
        } else checkBox.setChecked(false);

        new AlertDialog.Builder(getContext())
                .setView(view1)
                .setPositiveButton("提交", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RecordData newRecordData = new RecordData();
                        newRecordData.setName(nameEdit.getText().toString());
                        newRecordData.setIp(ipEdit.getText().toString());
                        newRecordData.setMac(macEdit.getText().toString().toUpperCase());
                        newRecordData.setUser(userEdit.getText().toString());
                        newRecordData.setPasswd(passwdEdit.getText().toString());
                        if (group.getCheckedRadioButtonId() == R.id.manual_type_bluetooth) {
                            newRecordData.setType("Bluetooth");
                            Pattern pattern = Pattern.compile(RegexTool.macRegex);
                            Matcher matcher = pattern.matcher(macEdit.getText().toString().toUpperCase());
                            if (!matcher.matches()) {
                                Toast.makeText(getContext(), "地址不合法", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else {
                            newRecordData.setType("WLAN");
                            Pattern pattern = Pattern.compile(RegexTool.ipRegex);
                            Matcher matcher = pattern.matcher(ipEdit.getText().toString());
                            if (!matcher.matches()) {
                                Toast.makeText(getContext(), "地址不合法", Toast.LENGTH_LONG).show();
                                return;
                            } else {
                                // ping，获取mac
                                String s = ARPInfo.getMACFromIPAddress(ipEdit.getText().toString());
                                if (s == null) {
//                                                Toast.makeText(getContext(),"未获取到ip对应的mac地址,不作更改",Toast.LENGTH_LONG).show();
                                } else {
                                    newRecordData.setMac(s.toUpperCase());
                                    Toast.makeText(getContext(), "更新成功并获取到Ip对应的Mac地址", Toast.LENGTH_LONG).show();
                                }

                            }
                        }
                        if (checkBox.isChecked()) {
                            newRecordData.setIsDefault(RecordData.TRUE);
                        } else newRecordData.setIsDefault(RecordData.FALSE);
                        //更新数据库
                        updateRecord(recordData, newRecordData);
                        //更新前端
                        int length = recordDataArrayList.size();
                        for (int i = 0; i < length; ++i) {

                            if (recordDataArrayList.get(i).getMac()==null || recordData.getMac()==null){
                                continue;
                            }

                            if (recordDataArrayList.get(i).getMac().equals(recordData.getMac())) {
                                recordDataArrayList.remove(i);
                                recordDataArrayList.add(i, newRecordData);
                            } else {
                                if (newRecordData.getIsDefault() == RecordData.TRUE) {
                                    //如果是默认的话，更新前端的"默认"显示
                                    RecordData data = recordDataArrayList.get(i);
                                    data.setIsDefault(RecordData.FALSE);
                                    recordDataArrayList.set(i, data);
                                }
                            }
                        }
                        update();
                    }

        }).setNegativeButton("取消",null)
        .show();
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == WRITE_PERMISSION) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            EasyPermissions.requestPermissions(this, "读取需要获取读写权限", WRITE_PERMISSION, permissions);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (helper!=null){
            helper.close();
        }
    }
}
