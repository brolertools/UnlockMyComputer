package com.kingtous.remotefingerunlock.ToolFragment;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;


import com.kingtous.remotefingerunlock.Common.FunctionTool;
import com.kingtous.remotefingerunlock.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ModeFragment extends Fragment {

    private RadioGroup group;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.mode_list,container,false);
        group=view.findViewById(R.id.manual_mode);
        getSettings();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                Log.d("模式",String.valueOf(i));
                switch (i){
                    case R.id.manual_mode_direct:
                        FunctionTool.editModes(getContext(),Integer.valueOf(FunctionTool.directMode));
                        break;
                    case R.id.manual_mode_remote:
                        FunctionTool.editModes(getContext(),Integer.valueOf(FunctionTool.remoteMode));
                        break;
                }
            }
        });
    }

    private void getSettings(){
        int modes=FunctionTool.detectModes(getContext());
        String mode=String.valueOf(modes);
        switch (mode){
            case FunctionTool.directMode:
                group.check(R.id.manual_mode_direct);
                break;
            case FunctionTool.remoteMode:
                group.check(R.id.manual_mode_remote);
                break;
        }
    }



}
