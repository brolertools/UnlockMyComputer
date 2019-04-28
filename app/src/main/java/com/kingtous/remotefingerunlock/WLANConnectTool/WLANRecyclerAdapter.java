package com.kingtous.remotefingerunlock.WLANConnectTool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kingtous.remotefingerunlock.R;

import org.w3c.dom.Text;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class WLANRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    int footer_postion=0;
    //原来加了footer的，现在不需要了
    int TYPE_FOOTER=0;
    int TYPE_DEVICES=1;

    ArrayList<WLANDeviceData> devices;
    View footer;

    WLANRecyclerAdapter(ArrayList<WLANDeviceData> list){
        devices=list;
    }


    //========接口============
    public interface OnItemClickListener{
        void OnClick(View view,int Position);
    }

    private WLANRecyclerAdapter.OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(WLANRecyclerAdapter.OnItemClickListener mOnItemClickListener){
        this.mOnItemClickListener = mOnItemClickListener;
    }
    //=========暴露接口=========

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType==footer_postion && footer!=null)
        {
            return new WLANRecyclerAdapter.deviceHolder(footer);
        }
        View layout= LayoutInflater.from(parent.getContext()).inflate(R.layout.wlan_device_item,parent,false);
        return new WLANRecyclerAdapter.deviceHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position)==TYPE_DEVICES){
            ((deviceHolder)holder).ip.setText(devices.get(position).getIp());
            ((deviceHolder)holder).mac.setText(devices.get(position).getMac());
            ((deviceHolder)holder).name.setText(devices.get(position).getName());
            //=======通过接口回调===========
            ((WLANRecyclerAdapter.deviceHolder) holder).cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.OnClick(((WLANRecyclerAdapter.deviceHolder) holder).cardView,position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (footer!=null)
        {
            return devices.size()+1;
        }
        else
            return devices.size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_DEVICES;
    }

    public class deviceHolder extends RecyclerView.ViewHolder{

        public CardView cardView;
        public TextView name;
        public TextView mac;
        public TextView ip;

        public deviceHolder(@NonNull View itemView) {
            super(itemView);
            if (itemView==footer)
                return;
            else {
                mac=itemView.findViewById(R.id.name_WLAN_device_mac);
                cardView=itemView.findViewById(R.id.card_WLAN);
                name=itemView.findViewById(R.id.name_WLAN_device_name);
                ip=itemView.findViewById(R.id.name_WLAN_device_ip);
            }
        }
    }
}
