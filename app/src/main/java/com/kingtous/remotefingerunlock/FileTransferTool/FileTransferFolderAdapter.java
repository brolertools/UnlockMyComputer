package com.kingtous.remotefingerunlock.FileTransferTool;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kingtous.remotefingerunlock.BluetoothConnectTool.BluetoothRecyclerAdapter;
import com.kingtous.remotefingerunlock.R;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class FileTransferFolderAdapter extends RecyclerView.Adapter {

    private FileModel model;

    static final int FILE = 0;
    static final int FOLDER = 1;

    FileTransferFolderAdapter(FileModel model){
        this.model=model;
    }

    public interface OnItemClickListener {
        void OnClick(View view, int Position);
    }
    public interface OnItemLongClickListener {
        void OnLongClick(View view, int Position);
    }

    private FileTransferFolderAdapter.OnItemClickListener mOnItemClickListener;
    private FileTransferFolderAdapter.OnItemLongClickListener mOnItemLongClickListener;

    public void setOnItemClickListener(FileTransferFolderAdapter.OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setOnItemLongClickListener(FileTransferFolderAdapter.OnItemLongClickListener mOnItemLongClickListener){
        this.mOnItemLongClickListener=mOnItemLongClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.file_transfer_folder_show_item,parent,false);
        return new myHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        ((myHolder)holder).fileName.setText(model.getDetail().get(position).getFile_name());
        switch (model.getDetail().get(position).getAttributes()){
            case FILE:
                ((myHolder)holder).attributes.setText("文件");
                break;
            case FOLDER:
                ((myHolder)holder).attributes.setText("文件夹");
                break;
            default:
                ((myHolder)holder).attributes.setText("未知");
        }

        ((myHolder)holder).layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.OnClick(v,position);
            }
        });
        ((myHolder)holder).layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOnItemLongClickListener.OnLongClick(v,position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return model.getDetail().size();
    }

    public class myHolder extends RecyclerView.ViewHolder{

        CardView layout;
        TextView fileName;
        TextView attributes;

        public myHolder(@NonNull View itemView) {
            super(itemView);
            layout=itemView.findViewById(R.id.file_transfer_folder_current_folder_item);
            fileName=itemView.findViewById(R.id.file_transfer_folder_current_folder_fileName);
            attributes=itemView.findViewById(R.id.file_transfer_folder_current_folder_attr);
        }
    }

}
