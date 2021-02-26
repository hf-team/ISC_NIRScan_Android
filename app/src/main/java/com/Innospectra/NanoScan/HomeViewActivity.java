package com.Innospectra.NanoScan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.ISCSDK.ISCNIRScanSDK;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

/**
 * Created by iris.lin on 2018/2/2.
 */

public class HomeViewActivity  extends Activity {
    private static Context mContext;
    private Button main_scan;
    private Button main_setting;
//    private ImageButton main_info;
//    private ImageButton main_setting;
    private static final int REQUEST_WRITE_STORAGE = 112;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        mContext = this;
        initComponent();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasPermission) {
                ActivityCompat.requestPermissions(HomeViewActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION
                                , Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_WRITE_STORAGE);
            }
        }

        updateHomeViewBtnStatus();
    }

    private void initComponent()
    {
        main_scan = findViewById(R.id.main_scan);
        main_setting = findViewById(R.id.main_setting);
//        main_info = (ImageButton)findViewById(R.id.main_info);
//        main_setting = (ImageButton)findViewById(R.id.main_setting);

        main_scan.setOnClickListener(main_scan_listenser);
//        main_info.setOnClickListener(main_info_listenser);
        main_setting.setOnClickListener(main_setting_listenser);
    }

    //20210226 zhaozz: 此处是为了在恢复界面时查看设备状态，来修改按钮的显示值
    @Override
    public void onResume(){
        super.onResume();
        updateHomeViewBtnStatus();
    }

    //20210226 zhaozz： 尝试主界面的按钮合并，若连接成功则显示解绑设备
    private void updateHomeViewBtnStatus(){
        if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null) != null) {
            main_scan.setVisibility(View.VISIBLE);
            main_setting.setText(getResources().getString(R.string.text_disconnect));
            main_setting.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        }
        else {
            main_scan.setVisibility(View.GONE);
            main_setting.setText(getResources().getString(R.string.text_connect));
            main_setting.setBackgroundColor(ContextCompat.getColor(mContext, R.color.normal_background));
            main_setting.setTextColor(ContextCompat.getColor(mContext, R.color.normal_text));
        }
    }


    private Button.OnClickListener main_scan_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            String token=getIntent().getStringExtra("token");
            Intent newscanhIntent = new Intent(mContext, ScanViewActivity.class);
            newscanhIntent.putExtra("main","main");
            newscanhIntent.putExtra("token",token);
            startActivity(newscanhIntent);
        }
    };

    private Button.OnClickListener main_info_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            Intent infoIntent = new Intent(mContext, InformationViewActivity.class);
            startActivity(infoIntent);
        }
    };

   private AlertDialog alertDialog;

    private Button.OnClickListener main_setting_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            String currentBtnText = main_setting.getText().toString();
            if (currentBtnText == getResources().getString(R.string.text_connect))
            {
                Intent selectIntent = new Intent(mContext, SelectDeviceViewActivity.class);
                startActivity(selectIntent);
            }
            else if(currentBtnText == getResources().getString(R.string.text_disconnect))
            {
                String mac = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
                alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, mac));

                alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        alertDialog.dismiss();
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, null);
                        updateHomeViewBtnStatus();
                    }
                });

                alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            else if(currentBtnText == getResources().getString(R.string.text_home))
            {}
            else
            {
                //TODO: Nothing
            }
        }
    };

    public static StoreCalibration storeCalibration = new StoreCalibration();
    public static class StoreCalibration
    {
        String device;
        byte[] storrefCoeff;
        byte[] storerefMatrix;
    }
}
