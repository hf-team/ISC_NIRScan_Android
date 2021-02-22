package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

import java.util.ArrayList;

import static com.ISCSDK.ISCNIRScanSDK.GetScanConfiguration;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;


/**
 * This activity controls the view for the Nano stored scan configurations.
 * These configurations have to be individually read from the Nano
 *
 * WARNING: This activity uses JNI function calls. It is important that the name and location of
 *          this activity remain unchanged or the Spectrum C library call will fail
 *
 * @author collinmast
 */
public class ScanConfigurationsViewActivity extends Activity {

    private static Context mContext;
    private ScanConfAdapter scanConfAdapter;
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> configs = new ArrayList<>();
    private ListView lv_configs;
    private int storedConfSize;
    private int receivedConfSize;
    private Menu mMenu;
    ProgressDialog barProgressDialog;
    public static Boolean saveConfig =false;
    public static ArrayList<ISCNIRScanSDK.ScanConfiguration> bufconfigs = new ArrayList<>();
    public static ArrayList<String>ScanConfigName = new ArrayList<>();
    public static ArrayList <byte []> bufEXTRADATA_fromScanConfigurationsViewActivity = new ArrayList<>();
    private ArrayList <byte []> EXTRADATA = new ArrayList<>();

    private final BroadcastReceiver ScanConfSizeReceiver = new ScanConfSizeReceiver();
    private final BroadcastReceiver GetActiveScanConfReceiver = new GetActiveScanConfReceiver();
    private final BroadcastReceiver ScanConfReceiver = new ScanConfReceiver();
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter scanConfSizeFilter = new IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE);
    private final IntentFilter getActiveScanConfFilter = new IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF);
    private final IntentFilter scanConfFilter = new IntentFilter(ISCNIRScanSDK.SCAN_CONF_DATA);
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ScanConfigurationsViewActivity.notifybackground";

    private static Boolean GotoOtherPage = false;
    @Override
    public void finishActivity(int requestCode) {
        super.finishActivity(requestCode);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_conf);
        ScanViewActivity.GotoScanConfigFlag = true;
        mContext = this;
        ScanConfigName.clear();
        lv_configs = (ListView) findViewById(R.id.lv_configs);
        lv_configs.setOnItemClickListener(Select_Config_ItemClick);
        //Set up the action bar title, and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.scan_configurations));
        }

        //Get the number of scan config and scan config data
        ISCNIRScanSDK.GetScanConfig();
        //register the necessary broadcast receivers
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfSizeReceiver, scanConfSizeFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfReceiver, scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetActiveScanConfReceiver, getActiveScanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }
    /**
     * Send broadcast  GET_SCAN_CONF will  through ScanConfSizeReceiver to get the number of scan config(ISCNIRScanSDK.GetScanConfig() should be claaed)
     */
    private class ScanConfSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            storedConfSize = intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0);
            if (storedConfSize > 0) {
                barProgressDialog = new ProgressDialog(ScanConfigurationsViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.reading_configurations));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
                receivedConfSize = 0;
            }
        }
    }
    /**
     * Send broadcast  GET_SCAN_CONF will  through ScanConfSizeReceiver to get the scan config data(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ISCNIRScanSDK.ScanConfiguration scanConf = GetScanConfiguration(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            EXTRADATA.add(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            receivedConfSize++;
            if (receivedConfSize == storedConfSize) {
                ISCNIRScanSDK.GetActiveConfig();
                mMenu.findItem(R.id.action_add).setEnabled(true);
            } else {
                barProgressDialog.setProgress(receivedConfSize);
            }
            configs.add(scanConf);
            ScanConfigName.add(scanConf.getConfigName());
        }
    }
    /**
     * Send broadcast  GET_ACTIVE_CONF will  through GetActiveScanConfReceiver to get active config(ISCNIRScanSDK.GetActiveConfig() should be called)
     */
    private class GetActiveScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int index = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)[0];
            barProgressDialog.dismiss();
            lv_configs.setVisibility(View.VISIBLE);
            scanConfAdapter = new ScanConfAdapter(mContext, configs);
            lv_configs.setAdapter(scanConfAdapter);
            for (ISCNIRScanSDK.ScanConfiguration c : scanConfAdapter.configs) {
                //get the first one byte
                int ScanConfigIndextoByte = (byte)c.getScanConfigIndex();
                if (c.getScanConfigIndex() == index  || ScanConfigIndextoByte == index) {
                    c.setActive(true);
                    lv_configs.setAdapter(scanConfAdapter);
                } else {
                    c.setActive(false);
                }
            }
        }
    }
    private ListView.OnItemClickListener Select_Config_ItemClick = new ListView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            byte[] index = {0, 0};
            index[0] = (byte) scanConfAdapter.configs.get(i).getScanConfigIndex();
            //the index over 256 should calculate index[1]
            index[1] = (byte) (scanConfAdapter.configs.get(i).getScanConfigIndex()/256);
            ISCNIRScanSDK.SetActiveConfig(index);
            for(int j=0;j<scanConfAdapter.configs.size();j++)
            {
                if(j==i)
                {
                    scanConfAdapter.configs.get(j).setActive(true);
                }
                else
                    scanConfAdapter.configs.get(j).setActive(false);
            }
            scanConfAdapter.notifyDataSetChanged();
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
        if(saveConfig == true)
        {
            configs.clear();
            EXTRADATA.clear();
            ISCNIRScanSDK.GetScanConfig();
            saveConfig = false;
        }
    }

    /**
     * When the activity is destroyed, unregister the BroadcastReceivers
     * handling receiving scan configurations, disconnect events, the # of configurations,
     * and the active configuration
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stored_configurations, menu);
        mMenu = menu;
        mMenu.findItem(R.id.action_add).setEnabled(false);
        //Tiva version <2.1.0.67
        if(ScanViewActivity.fw_level.compareTo(ScanViewActivity.FW_LEVEL.LEVEL_0)==0 || ISCNIRScanSDK.getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") ==false || ScanViewActivity.isOldTiva)
            mMenu.findItem(R.id.action_add).setVisible(false);
        return true;
    }

    /**
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            GotoOtherPage = true;
            Intent configureIntent = new Intent(mContext, AddScanConfigViewActivity.class);
            configureIntent.putExtra("Store config size",storedConfSize);
            configureIntent.putExtra("Serial Number",configs.get(0).getScanConfigSerialNumber());
            startActivity(configureIntent);
        }
        if (id == android.R.id.home) {
            GotoOtherPage = true;
            bufconfigs.clear();
            bufEXTRADATA_fromScanConfigurationsViewActivity.clear();
            for(int i=0;i<configs.size();i++)
            {
                bufconfigs.add(configs.get(i));
                bufEXTRADATA_fromScanConfigurationsViewActivity.add(EXTRADATA.get(i));
            }
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Custom adapter that holds {@link ISCNIRScanSDK.ScanConfiguration} objects for the listview
     */
    public class ScanConfAdapter extends ArrayAdapter<ISCNIRScanSDK.ScanConfiguration> {
        private final ArrayList<ISCNIRScanSDK.ScanConfiguration> configs;
        public ScanConfAdapter(Context context, ArrayList<ISCNIRScanSDK.ScanConfiguration> values) {
            super(context, -1, values);
            this.configs = values;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_scan_configuration_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.ConfigName = (TextView) convertView.findViewById(R.id.tv_config_name);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final ISCNIRScanSDK.ScanConfiguration config = getItem(position);
            if (config != null) {
                viewHolder.ConfigName.setText(config.getConfigName());
                if (config.isActive()) {
                    viewHolder.ConfigName.setTextColor(ContextCompat.getColor(mContext, R.color.active_conf));
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, config.getConfigName());
                } else {
                    viewHolder.ConfigName.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                }
            }
            return convertView;
        }
    }
    /**
     * View holder for the {@link ISCNIRScanSDK.ScanConfiguration} class
     */
    private class ViewHolder {
        private TextView ConfigName;
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
}
