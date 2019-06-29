package com.kingtous.remotefingerunlock.FileTransferTool;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.Common.ToastMessageTool;
import com.kingtous.remotefingerunlock.R;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Stack;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FileTransferFolderActivity extends AppCompatActivity implements FileTransferFolderAdapter.OnItemClickListener, FileTransferFolderAdapter.OnItemLongClickListener, FileTransferQueryTask.ReturnListener {


    TextView folderView;
    RecyclerView folderRecyclerView;
    FileTransferFolderAdapter adapter;
    FloatingActionButton fab_stop;
    FloatingActionButton fab_poweroff;
    Stack<String> folderStack=new Stack<>();
    FileModel model=new FileModel();
    int flags;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_transfer_folder_show);
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
        initModel();
        initView();
//        tryIon();
    }

    void initModel(){
        Intent intent=getIntent();
        String data=intent.getStringExtra("detail");
        flags=intent.getIntExtra("flags",0);
        model=new Gson().fromJson(data,FileModel.class);
        updateModel(model);
    }

    void updateModel(FileModel modelt){
        if (modelt==null)
            return;
        if (folderView != null)
            folderView.setText(modelt.getCurrent_folder());
        if (modelt.getDetail()==null){
            ToastMessageTool.tts(this,"未返回文件列表，请检查远程端是否正常");
            return;
        }
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
        fab_stop=findViewById(R.id.file_transfer_folder_fab_stop);
        fab_poweroff=findViewById(R.id.file_transfer_folder_fab_poweroff);
        fab_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 关闭连接
                if (SocketHolder.getSocket()!=null && !SocketHolder.getSocket().isClosed()){
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
                finish();
            }
        });
        fab_poweroff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FunctionTool.shutdown(FileTransferFolderActivity.this,getIntent().getStringExtra("ip"),getIntent().getStringExtra("mac"),flags);
            }
        });
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



//    public void tryIon() {
//        JsonObject object=new JsonObject();
//        object.addProperty("Query","/./etc");
//        Ion.with(this).load("123.206.34.50").setJsonObjectBody(object).asJsonObject().setCallback(new FutureCallback<JsonObject>() {
//            @Override
//            public void onCompleted(Exception e, JsonObject result) {
//                Log.d("D",result.toString());
//            }
//        });
//    }


    private void showErr(String message){
        new AlertDialog.Builder(this).setMessage(message).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).show();
    }

    @Override
    public void OnClick(View view, int Position) {
        Log.d("点击","短");
        FileModel.DetailBean detailBean=model.getDetail().get(Position);
        switch (detailBean.getAttributes()){
            case FileTransferFolderAdapter.FILE:
                // 请求文件大小
                try {
                    PropModel propModel=new FileTransferPropTask(this,getIntent().getStringExtra("ip"),getIntent().getStringExtra("mac")).execute(model.getCurrent_folder()+"/"+detailBean.getFile_name()).get();
                    detailBean.setSize(propModel.getFile_size());
                // 下载
                FileTransferDownTask downTask=
                        new FileTransferDownTask(this,getIntent().getStringExtra("ip"),model.getDetail().get(Position),getIntent().getStringExtra("mac"));
                downTask.execute(model.getCurrent_folder()+"/"+detailBean.getFile_name());
                } catch (Exception e){
                    showErr(e.getMessage());
                }
                break;
            case FileTransferFolderAdapter.FOLDER:
                //Query+更新

                // Win下会有.和..两个
                if (detailBean.getFile_name().equals(".")){
                    return;
                }

                folderStack.push(model.getCurrent_folder());
                String default_act_folder=model.getCurrent_folder()+"/"+detailBean.getFile_name();

                if (detailBean.getFile_name().equals("..")){
                    default_act_folder=model.getCurrent_folder().substring(0,model.getCurrent_folder().lastIndexOf("/"));
                }

                try {
                    FileTransferQueryTask task=new FileTransferQueryTask(this,getIntent().getStringExtra("ip"),getIntent().getStringExtra("mac"));
                    task.setmReturnListener(this);
                    task.execute(default_act_folder);
                } catch (Exception e){
                    showErr(e.getMessage());
                }
                break;
        }
    }

    @Override
    public void OnLongClick(View view, int Position) {
        if (model.getDetail().get(Position).getAttributes()==FileTransferFolderAdapter.FILE){
            FileModel.DetailBean detailBean=model.getDetail().get(Position);
            try {
                PropModel propModel=new FileTransferPropTask(this,getIntent().getStringExtra("ip"),getIntent().getStringExtra("mac")).execute(model.getCurrent_folder()+"/"+detailBean.getFile_name()).get();
                View v=LayoutInflater.from(this).inflate(R.layout.file_transfer_file_item_info,null,false);
                ((TextView)v.findViewById(R.id.file_name)).setText(propModel.getFile_name());
                ((TextView)v.findViewById(R.id.file_size)).setText(String.valueOf(((double)propModel.getFile_size())/1024)+"KB");
                new AlertDialog.Builder(this)
                        .setView(v)
                        .setPositiveButton("确定",null).show();
            } catch (Exception e){
                showErr(e.getMessage());
            }
        }
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


    @Override
    public void onBackPressed() {
        if (folderStack.size()>0){
            String folder=folderStack.pop();
            try {
                FileTransferQueryTask task=new FileTransferQueryTask(this,getIntent().getStringExtra("ip"),getIntent().getStringExtra("mac"));
                task.setmReturnListener(this);
                task.execute(folder);
            } catch (Exception e){
                showErr(e.getMessage());
            }
        }
        else
            super.onBackPressed();
    }

    @Override
    public void onReturnListener(FileModel model) {
        updateModel(model);
    }
}
