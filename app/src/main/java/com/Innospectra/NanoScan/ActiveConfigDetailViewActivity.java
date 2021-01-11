package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

import java.util.ArrayList;

public class ActiveConfigDetailViewActivity extends Activity {
    private static Context mContext;
    private ScanConfAdapter scanConfAdapter;
    private SlewScanConfAdapter slewScanConfAdapter;
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> configs = new ArrayList<>();
    private ArrayList<ISCNIRScanSDK.SlewScanSection> sections = new ArrayList<>();
    private ListView lv_configs;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    ArrayList<String> exposure_time_vlaue = new ArrayList<>();
    String widthnm[] ={"","","2.34","3.51","4.68","5.85","7.03","8.20","9.37","10.54","11.71","12.88","14.05","15.22","16.39","17.56","18.74"
    ,"19.91","21.08","22.25","23.42","24.59","25.76","26.93","28.10","29.27","30.44","31.62","32.79","33.96","35.13","36.30","37.47","38.64","39.81"
    ,"40.98","42.15","43.33","44.50","45.67","46.84","48.01","49.18","50.35","51.52","52.69","53.86","55.04","56.21","57.38","58.55","59.72","60.89"};
    private static Boolean GotoOtherPage = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_scan);
        init_exposure_time_value();
        mContext = this;
        ScanViewActivity.showActiveconfigpage = true;
        ISCNIRScanSDK.ScanConfiguration activeConf = null;
        if(getIntent().getSerializableExtra("conf") != null){
            activeConf = (ISCNIRScanSDK.ScanConfiguration) getIntent().getSerializableExtra("conf");
        }
        //Set up the action bar title, and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            if(activeConf != null) {
                ab.setTitle(activeConf.getConfigName());
            }
        }
        lv_configs = (ListView) findViewById(R.id.lv_configs);
        if(activeConf != null && activeConf.getScanType().equals("Slew")){
            int numSections = activeConf.getSlewNumSections();
            int i;
            for(i = 0; i < numSections; i++){
                sections.add(new ISCNIRScanSDK.SlewScanSection(activeConf.getSectionScanType()[i],
                        activeConf.getSectionWidthPx()[i],
                        (activeConf.getSectionWavelengthStartNm()[i] & 0xFFFF),
                        (activeConf.getSectionWavelengthEndNm()[i] & 0xFFFF),
                        activeConf.getSectionNumPatterns()[i] & 0x0FFF,
                         activeConf.getSectionNumRepeats()[i],
                        activeConf.getSectionExposureTime()[i]  & 0x000F));
            }
            Log.i("__ACTIVE_CONF","Setting slew conf adapter");
            slewScanConfAdapter = new SlewScanConfAdapter(mContext, sections);
            lv_configs.setAdapter(slewScanConfAdapter);
        }else{
            configs.add(activeConf);
            scanConfAdapter = new ScanConfAdapter(mContext, configs);
            lv_configs.setAdapter(scanConfAdapter);
        }
        //register the necessary broadcast receivers
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
    }
    private void init_exposure_time_value()
    {
        exposure_time_vlaue.add("0.635");
        exposure_time_vlaue.add("1.27");
        exposure_time_vlaue.add("2.54");
        exposure_time_vlaue.add("5.08");
        exposure_time_vlaue.add("15.24");
        exposure_time_vlaue.add("30.48");
        exposure_time_vlaue.add("60.96");
    }
    /*
     * On resume, make a call to the super class.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }

    /*
     * When the activity is destroyed, unregister the BroadcastReceivers
     * handling receiving scan configurations, disconnect events, the # of configurations,
     * and the active configuration
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

    /*
     * Inflate the options menu
     * In this case, there is no menu and only an up indicator,
     * so the function should always return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /*
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
                viewHolder.scanType = (TextView) convertView.findViewById(R.id.tv_scan_type);
                viewHolder.rangeStart = (TextView) convertView.findViewById(R.id.tv_range_start_value);
                viewHolder.rangeEnd = (TextView) convertView.findViewById(R.id.tv_range_end_value);
                viewHolder.width = (TextView) convertView.findViewById(R.id.tv_width_value);
                viewHolder.patterns = (TextView) convertView.findViewById(R.id.tv_patterns_value);
                viewHolder.repeats = (TextView) convertView.findViewById(R.id.tv_repeats_value);
                viewHolder.serial = (TextView) convertView.findViewById(R.id.tv_serial_value);
                viewHolder.scanType.setVisibility(View.GONE);
                LinearLayout ll_range_start = (LinearLayout)convertView.findViewById(R.id.ll_range_start);
                LinearLayout ll_range_end = (LinearLayout)convertView.findViewById(R.id.ll_range_end);
                LinearLayout ll_patterns= (LinearLayout)convertView.findViewById(R.id.ll_patterns);
                LinearLayout ll_width = (LinearLayout)convertView.findViewById(R.id.ll_width);
                ll_range_start.setVisibility(View.VISIBLE);
                ll_range_end.setVisibility(View.VISIBLE);
                ll_patterns.setVisibility(View.VISIBLE);
                ll_width.setVisibility(View.VISIBLE);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final ISCNIRScanSDK.ScanConfiguration config = getItem(position);
            if (config != null) {
                int widthindex = (int)config.getWidthPx();
                viewHolder.scanType.setText(config.getConfigName());
                viewHolder.rangeStart.setText(getString(R.string.range_start_value, config.getWavelengthStartNm()));
                viewHolder.rangeEnd.setText(getString(R.string.range_end_value, config.getWavelengthEndNm()));
                viewHolder.width.setText(widthnm[widthindex] + " nm");
                viewHolder.patterns.setText(getString(R.string.patterns_value, config.getNumPatterns()));
                viewHolder.repeats.setText(getString(R.string.repeats_value, config.getNumRepeats()));
                viewHolder.serial.setText(config.getScanConfigSerialNumber());
            }
            return convertView;
        }
    }

    /**
     * Custom adapter that holds {@link ISCNIRScanSDK.ScanConfiguration} objects for the listview
     */
    public class SlewScanConfAdapter extends ArrayAdapter<ISCNIRScanSDK.SlewScanSection> {
        private final ArrayList<ISCNIRScanSDK.SlewScanSection> sections;
        public SlewScanConfAdapter(Context context, ArrayList<ISCNIRScanSDK.SlewScanSection> values) {
            super(context, -1, values);
            this.sections = values;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_slew_scan_configuration_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.scanType = (TextView) convertView.findViewById(R.id.tv_scan_type);
                viewHolder.rangeStart = (TextView) convertView.findViewById(R.id.tv_range_start_value);
                viewHolder.rangeEnd = (TextView) convertView.findViewById(R.id.tv_range_end_value);
                viewHolder.width = (TextView) convertView.findViewById(R.id.tv_width_value);
                viewHolder.patterns = (TextView) convertView.findViewById(R.id.tv_patterns_value);
                viewHolder.repeats = (TextView) convertView.findViewById(R.id.tv_repeats_value);
                viewHolder.serial = (TextView) convertView.findViewById(R.id.tv_serial_value);
                viewHolder.exposure = (TextView)convertView.findViewById(R.id.tv_active_exposure_value);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final ISCNIRScanSDK.SlewScanSection config = getItem(position);
            if (config != null) {
                int widthindex = (int)config.getWidthPx();
                viewHolder.rangeStart.setText(getString(R.string.range_start_value, config.getWavelengthStartNm()));
                viewHolder.rangeEnd.setText(getString(R.string.range_end_value, config.getWavelengthEndNm()));
                viewHolder.width.setText(widthnm[widthindex] + " nm");
                viewHolder.patterns.setText(getString(R.string.patterns_value, config.getNumPatterns()));
                viewHolder.repeats.setText(getString(R.string.repeats_value, config.getNumRepeats()));
                int index = config.getExposureTime();
                viewHolder.exposure.setText(exposure_time_vlaue.get(index) + " ms");
            }
            return convertView;
        }
    }

    /**
     * View holder for the {@link ISCNIRScanSDK.ScanConfiguration} class
     */
    private class ViewHolder {
        private TextView scanType;
        private TextView rangeStart;
        private TextView rangeEnd;
        private TextView width;
        private TextView patterns;
        private TextView repeats;
        private TextView serial;
        private TextView exposure;
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
