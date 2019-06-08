package com.kingtous.remotefingerunlock.FileTransferTool;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.kingtous.remotefingerunlock.R;
import com.kingtous.remotefingerunlock.Security.SSLSecurityClient;
import com.kingtous.remotefingerunlock.WLANConnectTool.WLANDeviceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.Comparator;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FileTransferFolderActivity extends AppCompatActivity implements FileTransferFolderAdapter.OnItemClickListener, FileTransferFolderAdapter.OnItemLongClickListener {


    TextView folderView;
    RecyclerView folderRecyclerView;
    FileTransferFolderAdapter adapter;
    Stack<String> folderStack=new Stack<>();
    FileModel model=new FileModel();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_transfer_folder_show);
        initModel();
        initView();
    }

    void initModel(){
        Intent intent=getIntent();
        String data=intent.getStringExtra("detail");
        model=new Gson().fromJson(data,FileModel.class);
        updateModel(model);
    }

    void updateModel(FileModel modelt){
        if (folderView!=null)
            folderView.setText(modelt.getCurrent_folder());
        modelt.getDetail().sort(new Comparator<FileModel.DetailBean>() {
            @Override
            public int compare(FileModel.DetailBean o1, FileModel.DetailBean o2) {
                if (o1.getAttributes()==o2.getAttributes()){
                    return o1.getFile_name().compareTo(o2.getFile_name());
                }
                else {
                    if (o1.getAttributes()<o2.getAttributes()){
                        return 1;
                    }
                    else return -1;
                }
            }
        });
        //替换Model中的元素，不能直接=
        model.setCurrent_folder(modelt.getCurrent_folder());
        model.setDetail(modelt.getDetail());
        if (adapter!=null)
            adapter.notifyDataSetChanged();
    }

    void initView(){
        folderView=findViewById(R.id.file_transfer_folder_current_folder);
        folderRecyclerView=findViewById(R.id.file_transfer_folder_recyclerview);
        LinearLayoutManager manager=new LinearLayoutManager(this);
        folderRecyclerView.setLayoutManager(manager);

        if (model!=null){
            folderView.setText(model.getCurrent_folder());
            adapter=new FileTransferFolderAdapter(model);
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            folderRecyclerView.setAdapter(adapter);
        }
        else {
            Log.e("model","不存在，无法显示");
        }

    }

    @Override
    public void OnClick(View view, int Position) {
        Log.d("点击","短");
        FileModel.DetailBean detailBean=model.getDetail().get(Position);
        switch (detailBean.getAttributes()){
            case FileTransferFolderAdapter.FILE:
                //下载
                Log.d("假装在下载","666");
                break;
            case FileTransferFolderAdapter.FOLDER:
                //Query+更新
                folderStack.push(model.getCurrent_folder());
                try {
                    FileModel modelt=new FileTransferQueryTask(this,getIntent().getStringExtra("ip")).execute(model.getCurrent_folder()+"/"+detailBean.getFile_name()).get();
                    updateModel(modelt);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void OnLongClick(View view, int Position) {
        Log.d("点击","长");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketHolder.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
