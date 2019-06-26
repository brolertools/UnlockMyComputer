package com.kingtous.remotefingerunlock.ToolFragment;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.kingtous.remotefingerunlock.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ModeFragment extends Fragment {



    private RadioGroup group;
    private RadioButton btn_direct;
    private RadioGroup btn_remote;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.mode_list,container,false);
        group=view.findViewById(R.id.manual_mode);
        btn_direct=view.findViewById(R.id.manual_mode_direct);
        btn_remote=view.findViewById(R.id.manual_mode_remote);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                Log.d("模式",String.valueOf(i));
            }
        });

    }
}
