package com.kingtous.remotefingerunlock.Application;

import android.app.Application;
import android.content.SharedPreferences;

import com.tencent.bugly.crashreport.CrashReport;

public class myApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "f18c887505", true);

        //初始化设置
//        SharedPreferences preferences
    }
}
