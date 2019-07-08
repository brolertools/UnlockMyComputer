package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kingtous.remotefingerunlock.Common.FunctionTool.MIME_MapTable;

public class FileListActivity extends ListActivity {

    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);
        Toolbar toolbar= (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.back2);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(getResources().getColor(R.color.deepskyblue));

        // Use the current directory as title
        path = "/";
        if (getIntent().hasExtra("path")) {
            path = getIntent().getStringExtra("path");
        }
        setTitle(path);

        // Read all files sorted into the values-array
        List values = new ArrayList();
        File dir = new File(path);
        if (!dir.canRead()) {
            setTitle(getTitle() + " (inaccessible)");
        }
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                if (!file.startsWith(".")) {
                    values.add(file);
                }
            }
        }
        Collections.sort(values);

        // Put the data into the list
        ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String filename = (String) getListAdapter().getItem(position);
        if (path.endsWith(File.separator)) {
            filename = path + filename;
        } else {
            filename = path + File.separator + filename;
        }
        if (new File(filename).isDirectory()) {
            Intent intent = new Intent(this, FileListActivity.class);
            intent.putExtra("path", filename);
            startActivity(intent);
        } else {
            String finalFilename = filename;
            new AlertDialog.Builder(this)
                    .setMessage("请选择操作")
                    .setPositiveButton("打开", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            processFile(finalFilename);
                        }
                    })
                    .setNeutralButton("发送", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendFile(finalFilename);
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

    private void sendFile(String fileName){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_SEND);
        // 获得拓展名
        String type="*/*";
        String[] tmp=fileName.split("\\.");
        if (tmp.length!=1){
            for (String [] pair:MIME_MapTable){
                if (pair[0].equals(tmp[tmp.length-1])) {
                    type=pair[1];
                    break;
                }
            }
        }
        File file=new File(fileName);
        Uri contentUri= FileProvider.getUriForFile(this,this.getPackageName()+".fileprovider",file);
        intent.putExtra(Intent.EXTRA_STREAM,contentUri);
        intent.setType(type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivity(intent);
        }
        catch (ActivityNotFoundException e){
            ToastMessageTool.tts(this,"没有能打开此类型的应用");
        }
    }

    private void processFile(String fileName){
        Intent intent=new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        // 获得拓展名
        String type= FunctionTool.getFormatNameOfFile(fileName);
        File file=new File(fileName);
        Uri contentUri= FileProvider.getUriForFile(this,this.getPackageName()+".fileprovider",file);
        intent.setDataAndType(contentUri,type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivity(intent);
        }
        catch (ActivityNotFoundException e){
            ToastMessageTool.tts(this,"没有能打开此类型的应用");
        }
    }
}