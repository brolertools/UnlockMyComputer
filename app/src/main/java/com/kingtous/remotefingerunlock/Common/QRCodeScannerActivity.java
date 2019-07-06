package com.kingtous.remotefingerunlock.Common;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kingtous.remotefingerunlock.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import cn.bingoogolapple.qrcode.core.BarcodeType;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class QRCodeScannerActivity extends AppCompatActivity implements QRCodeView.Delegate {


    ZXingView zXingView;
    FloatingActionButton btn_flashlight;

    int flash_ligtht=0;

    public static int OK=0;
    public static int ERR=-1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        zXingView=findViewById(R.id.qrscanner);
        zXingView.setType(BarcodeType.ONLY_QR_CODE,null);


        btn_flashlight=findViewById(R.id.qrscanner_flashlight);
        zXingView.setDelegate(this);
        setResult(ERR);

        Toolbar toolbar= findViewById(R.id.toolbar);
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

        btn_flashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flash_ligtht==0){
                    zXingView.openFlashlight();
                    flash_ligtht=1;
                    btn_flashlight.setImageResource(android.R.drawable.presence_online);
                }
                else {
                    zXingView.closeFlashlight();
                    flash_ligtht=0;
                    btn_flashlight.setImageResource(android.R.drawable.presence_invisible);
                }
            }
        });
    }

    private void startScan(){
        zXingView.startCamera();
        zXingView.getScanBoxView().setTipText("请将设备二维码放入框中");
        zXingView.startSpotAndShowRect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startScan();
    }

    @Override
    protected void onDestroy() {
        zXingView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        zXingView.stopCamera();
        super.onStop();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        try {
            JsonObject object=new Gson().fromJson(result,JsonObject.class);
            if (object.has("mac") && object.has("ip") && object.has("hostname")){
                vibrate();

                // 判断MAC地址是以什么格式返回的
                String macTemp=object.get("mac").getAsString();
                if (!RegexTool.isStdMac(macTemp)){
                    if (macTemp.length()!=12){
                        throw new JsonSyntaxException(getString(R.string.notValidQRCode));
                    }
                    String macT=macTemp.substring(0,2);
                    for (int index=2;index<macTemp.length();index=index+2){
                            macT=macT+":"+macTemp.substring(index,index+2);
                    }
                    object.addProperty("mac",macT);
                }

                Intent intent=new Intent();
                intent.putExtra("result",object.toString());
                setResult(OK,intent);
                finish();
            }
            else {
                throw new JsonSyntaxException(getString(R.string.notValidQRCode));
            }
        }catch (JsonSyntaxException e){
            new AlertDialog.Builder(this)
                    .setMessage("不是有效的二维码")
                    .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            zXingView.startSpotAndShowRect();
                        }
                    })
                    .setCancelable(false)
                    .show();
            setResult(ERR);
        }
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
        String tipText = zXingView.getScanBoxView().getTipText();
        String ambientBrightnessTip = "\n\n环境过暗，请打开闪光灯";
        if (isDark) {
            if (!tipText.contains(ambientBrightnessTip)) {
                zXingView.getScanBoxView().setTipText(tipText + ambientBrightnessTip);
            }
        } else {
            if (tipText.contains(ambientBrightnessTip)) {
                tipText = tipText.substring(0, tipText.indexOf(ambientBrightnessTip));
                zXingView.getScanBoxView().setTipText(tipText);
            }
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e("QRCODE:", "打开相机出错");

    }
}
