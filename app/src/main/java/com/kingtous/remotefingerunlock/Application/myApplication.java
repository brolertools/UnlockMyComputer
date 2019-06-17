package com.kingtous.remotefingerunlock.Application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kingtous.remotefingerunlock.R;
import com.tencent.bugly.crashreport.CrashReport;

public class myApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "f18c887505", true);

        //初始化设置
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor=preferences.edit();
        String conn_mode=preferences.getString(getString(R.string.connect_mode),"0");
        editor.putString(getString(R.string.connect_mode),conn_mode);
        editor.apply();

    }
}
