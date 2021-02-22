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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.ISCSDK.ISCNIRScanSDK;

import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;


/**
 * This activity controls the view for settings once the Nano is connected
 * Four options are presented, each one launching a new activity.
 * Since each option requires the Nano to be connected to perform GATT operations,
 *
 * @author collinmast
 */
public class ConfigureViewActivity extends Activity {

    private static Context mContext;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.Configuration.notifybackground";
    private LinearLayout ll_device_info;
    private LinearLayout ll_device_status;
    private LinearLayout ll_scan_config;
    private LinearLayout ll_lock_button;
    private ToggleButton toggle_btn_lock_button;
    private View view_lock_button;
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        mContext = this;
        //Set the action bar title and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.configure));
        }
        InitComponent();
        //Register the disconnect broadcast receiver
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }
    private void InitComponent()
    {
        ll_device_info = (LinearLayout)findViewById(R.id.ll_device_info);
        ll_device_status = (LinearLayout)findViewById(R.id.ll_device_status);
        ll_scan_config = (LinearLayout)findViewById(R.id.ll_scan_config);
        ll_lock_button = (LinearLayout)findViewById(R.id.ll_lock_button);
        toggle_btn_lock_button = (ToggleButton)findViewById(R.id.btn_lock_button);
        view_lock_button = (View)findViewById(R.id.view_lock_button);

        ll_device_info.setOnClickListener(View_Click);
        ll_device_status.setOnClickListener(View_Click);
        ll_scan_config.setOnClickListener(View_Click);
        toggle_btn_lock_button.setOnCheckedChangeListener(Toggle_Button_OnCheckedChanged);

        if(ScanViewActivity.fw_level.compareTo(ScanViewActivity.FW_LEVEL.LEVEL_1)<=0 || ISCNIRScanSDK.getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") ==false|| ScanViewActivity.isOldTiva)
        {
            ll_lock_button.setVisibility(View.GONE);
            view_lock_button.setVisibility(View.GONE);
        }
        else
        {
            Boolean isLockScan = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,true);
            toggle_btn_lock_button.setChecked(isLockScan);
        }
    }
    private LinearLayout.OnClickListener View_Click = new LinearLayout.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.ll_device_info:
                    GotoOtherPage = true;
                    Intent infoIntent = new Intent(mContext, DeviceInfoViewActivity.class);
                    startActivity(infoIntent);
                    break;
                case R.id.ll_device_status:
                    GotoOtherPage = true;
                    Intent statusIntent = new Intent(mContext, DeviceStatusViewActivity.class);
                    startActivity(statusIntent);
                    break;
                case R.id.ll_scan_config:
                    GotoOtherPage = true;
                    Intent confIntent = new Intent(mContext, ScanConfigurationsViewActivity.class);
                    startActivity(confIntent);
                    break;
            }
        }
    };
    ToggleButton.OnCheckedChangeListener Toggle_Button_OnCheckedChanged = new ToggleButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,toggle_btn_lock_button.isChecked());
            SetDeviceButtonStatus(toggle_btn_lock_button.isChecked());
        }
    };
    /**
     * On resume, make a call to the super class.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }
    /**
     * When the activity is destroyed, unregister the BroadcastReceiver
     * handling disconnection events.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
    }
    @Override
    public void onPause() {
        super.onPause();
        if(!GotoOtherPage)
        {
            Intent notifybackground = new Intent(ScanViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground);
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
        if (id == android.R.id.home) {
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
            finish();
        }
    }

    /**
     *  Set device physical button status
     */
    private void SetDeviceButtonStatus(Boolean isLockButton)
    {
        //User open lock button on Configure page
        if(isLockButton)
        {
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
        }
        else
        {
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        }
    }
}
