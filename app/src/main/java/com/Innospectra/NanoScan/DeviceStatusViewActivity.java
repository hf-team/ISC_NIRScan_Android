package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

/**
 * This activity controls the view for the Nano device status
 * This includes information such as Nano temp/humidity, and battery percentage
 *
 * @author collinmast
 */
public class DeviceStatusViewActivity extends Activity {
    private static Context mContext;
    private TextView tv_batt;
    private TextView tv_temp;
    private TextView tv_humid;
    private TextView tv_lamptime;
    Button btn_update_thresholds;
    private Button btn_device_status;
    private Button btn_error_status;
    private BroadcastReceiver mStatusReceiver;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.DeviceStatusViewActivity.notifybackground";
    String devStatus = "";
    byte[] devbyte;
    byte[] errbyte;
    String errorStatus = "";
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);
        mContext = this;
        //Set up the action bar title and enable the back arrow
        ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_status));
        }
        tv_batt = (TextView)findViewById(R.id.tv_batt);
        tv_temp = (TextView)findViewById(R.id.tv_temp);
        tv_humid = (TextView)findViewById(R.id.tv_humid);
        tv_lamptime = (TextView)findViewById(R.id.tv_lamptime);
        btn_update_thresholds = (Button) findViewById(R.id.btn_update_thresholds);
        btn_device_status = (Button)findViewById(R.id.btn_device_status);
        btn_error_status = (Button)findViewById(R.id.btn_error_status);

        btn_update_thresholds.setOnClickListener(Update_Threshold_Click);
        btn_device_status.setOnClickListener(Device_Status_Click);
        btn_error_status.setOnClickListener(Error_Status_Click);

        //Get device status information from the device
        ISCNIRScanSDK.GetDeviceStatus();
        setActivityTouchDisable(true);
        /**
                 * Set up receiver for device status information(ISCNIRScanSDK.GetDeviceStatus() should be called)
                 */
        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int batt = intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0);
                float temp = intent.getFloatExtra(ISCNIRScanSDK.EXTRA_TEMP, 0);
                float humid = intent.getFloatExtra(ISCNIRScanSDK.EXTRA_HUMID, 0);
                long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME,0);
                devStatus = intent.getStringExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS);
                errorStatus = intent.getStringExtra(ISCNIRScanSDK.EXTRA_ERR_STATUS);
                devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
                errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
                tv_batt.setText(getString(R.string.batt_level_value, batt));
                tv_temp.setText(getString(R.string.temp_value_c, Float.toString(temp)));
                tv_humid.setText(getString(R.string.humid_value,Float.toString(humid)));
                tv_lamptime.setText(GetLampTimeString(lamptime));
                ProgressBar pb = (ProgressBar)findViewById(R.id.pb_status);
                pb.setVisibility(View.INVISIBLE);
                setActivityTouchDisable(false);
            }
        };
        //Register receivers for disconnection events and device status information
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mStatusReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }
    public static String GetLampTimeString(long lamptime)
    {
        String lampusage = "";
        if (lamptime / 86400 != 0)
        {
            lampusage += lamptime / 86400 + "day ";
            lamptime -= 86400 * (lamptime / 86400);
        }
        if (lamptime / 3600 != 0)
        {
            lampusage += lamptime / 3600 + "hr ";
            lamptime -= 3600 * (lamptime / 3600);
        }
        if (lamptime / 60 != 0)
        {
            lampusage += lamptime / 60 + "min ";
            lamptime -= 60 * (lamptime / 60);
        }
        lampusage += lamptime + "sec ";
        return lampusage;
    }
    //region Button vent
    private Button.OnClickListener Update_Threshold_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            ProgressBar pb = (ProgressBar)findViewById(R.id.pb_status);
            pb.setVisibility(View.VISIBLE);
            setActivityTouchDisable(true);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ISCNIRScanSDK.GET_STATUS));
        }
    };
    private Button.OnClickListener Device_Status_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            GotoOtherPage = true;
            Intent graphIntent = new Intent(mContext, AdvanceDeviceStatusViewActivity.class);
            graphIntent.putExtra("DEVSTATUS", devStatus);
            graphIntent.putExtra("DEVBYTE",devbyte);
            startActivity(graphIntent);
        }
    };

    private Button.OnClickListener Error_Status_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            GotoOtherPage = true;
            Intent graphIntent = new Intent(mContext, ErrorStatusViewActivity.class);
            graphIntent.putExtra("ERRSTATUS", errorStatus);
            graphIntent.putExtra("ERRBYTE",errbyte);
            startActivity(graphIntent);
        }
    };
    //endregion
    /**
         * On resume, make a call to the super class.
         * Nothing else is needed here besides calling
         * the super method.
         */
    @Override
    public void onResume(){
        super.onResume();
        GotoOtherPage = false;
    }

    /**
         * When the activity is destroyed, unregister the BroadcastReceivers
         * handling disconnection and status events
         */
    @Override
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(!GotoOtherPage)
        {
            Intent notifybackground = new Intent(ScanViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground);
            Intent notifybackground2 = new Intent(ConfigureViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground2);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    private class  BackGroundReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }
    /**
         * Inflate the options menu
         * In this case, there is no menu and only an up indicator,
         * so the function should always return true.
         */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
         * Handle the selection of a menu item.
         * In this case, there is only the up indicator. If selected, this activity should finish.
         */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == android.R.id.home){
            GotoOtherPage = true;
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link HomeViewActivity}
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void setActivityTouchDisable(boolean value) {
        if (value) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
}
