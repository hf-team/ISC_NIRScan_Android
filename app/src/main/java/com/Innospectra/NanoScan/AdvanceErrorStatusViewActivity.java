package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by iris.lin on 2019/8/28.
 */

public class AdvanceErrorStatusViewActivity extends Activity {
    private ListView listView;
    private static Boolean GotoOtherPage = false;
    Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_scan_status);
        mContext = this;
        Bundle bundle = getIntent().getExtras();
        int pos =  bundle.getInt("POS");
        //Set up the action bar title and enable the back arrow
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            switch (pos)
            {
                case 0://Scan
                    ab.setTitle(getString(R.string.detail_scan_error_status));
                    break;
                case 1://ADC
                    ab.setTitle(getString(R.string.detail_adc_error_status));
                    break;
                case 5://Hardware
                    ab.setTitle(getString(R.string.detail_hw_error_status));
                    break;
                case 6://TMP006
                    ab.setTitle(getString(R.string.detail_tmp006_error_status));
                    break;
                case 7://HDC1000
                    ab.setTitle(getString(R.string.detail_hdc1000_error_status));
                    break;
                case 8://Battery
                    ab.setTitle(getString(R.string.detail_battery_error_status));
                    break;
                case 11://System
                    ab.setTitle(getString(R.string.detail_system_error_status));
                    break;
            }

        }

        byte[] errbyte = bundle.getByteArray("ERRBYTE");
        int[]  images0 = {R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};
        int[]  images1 = {R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};
        int[]  images5 = {R.drawable.leg_gray};
        int[]  images6 = {R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};
        int[]  images7 = {R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};
        int[]  images8 = {R.drawable.leg_gray};
        int[]  images11 = {R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};

        listView = (ListView) findViewById(R.id.error_scan_status_listview);
        String[] title0 = {"DLPC150 Boot Error", "DLPC150 Init Error", "DLPC150 Lamp Driver Error", "DLPC150 Crop Image Failed", "ADC Data Error", "CFG Invalid", "Scan Pattern Streaming", "DLPC150 Read Error"};
        String[] title1 = {"Timeout", "Power Down", "Power Up", "Standby", "Wake up", "Read Register", "Write Register", "Configure", "Set Buffer","Command"};
        String[] title5 = {"DLPC150"};
        String[] title6 = {"Manufacturing Id", "Device Id", "Reset", "Read Register", "Write Register", "Timeout", "I2C"};
        String[] title7 = {"Manufacturing Id", "Device Id", "Reset", "Read Register", "Write Register", "Timeout", "I2C"};
        String[] title8 = {"Under Voltage"};
        String[] title11 = {"Unstable Lamp ADC", "Unstable Peak Intensity", "ADS1255 Error", "Auto PGA Error"};
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        switch (pos) {
            case 0://Scan
                int data0 = errbyte[4]&0xFF;//avoid negative number
                int error_scan0 = 0x00000001;
                for (int j = 0; j < 8; j++) {
                    int ret = data0 & error_scan0;
                    if (ret == error_scan0) {
                        images0[j] = R.drawable.led_r;
                    }
                    error_scan0 = error_scan0 << 1;
                }
                for (int i = 0; i < images0.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images0[i]);
                    map.put("title", title0[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 1://ADC
                int data1 = errbyte[5];
                for(int i=9;i>=0;i--)
                {
                    if(data1>=(i+1))
                    {
                        images1[i] = R.drawable.led_r;
                        data1 -= (i+1);
                    }
                }
                for (int i = 0; i < images1.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images1[i]);
                    map.put("title", title1[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 5://Hardware
                int data5 = errbyte[11];
                if(data5 == 1)
                {
                    images5[0] = R.drawable.led_r;
                }
                for (int i = 0; i < images5.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images5[i]);
                    map.put("title", title5[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 6://TMP006
                int data6 = errbyte[12];
                for(int i=6;i>=0;i--)
                {
                    if(data6>=(i+1))
                    {
                        images6[i] = R.drawable.led_r;
                        data6 -= (i+1);
                    }
                }
                for (int i = 0; i < images6.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images6[i]);
                    map.put("title", title6[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 7://HDC1000
                int data7 = errbyte[13];
                for(int i=6;i>=0;i--)
                {
                    if(data7>=(i+1))
                    {
                        images7[i] = R.drawable.led_r;
                        data7 -= (i+1);
                    }
                }
                for (int i = 0; i < images7.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images7[i]);
                    map.put("title", title7[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 8://Battery
                int data8 = errbyte[14];
                if(data8 == 1)
                {
                    images8[0] = R.drawable.led_r;
                }
                for (int i = 0; i < images8.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images8[i]);
                    map.put("title", title8[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
            case 11://System
                int data11 = errbyte[17]&0xFF;//avoid negative number
                int error_scan11 = 0x00000001;
                for (int j = 0; j < 4; j++) {
                    int ret = data11 & error_scan11;
                    if (ret == error_scan11) {
                        images11[j] = R.drawable.led_r;
                    }
                    error_scan11 = error_scan11 << 1;
                }
                for (int i = 0; i < images11.length; i++) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("images", images11[i]);
                    map.put("title", title11[i]);
                    list.add(map);
                    SimpleAdapter adapter = new SimpleAdapter(this, list,
                            R.layout.activity_error_status_item, new String[]{"images", "title"}, new int[]{
                            R.id.image, R.id.error_test});
                    listView.setAdapter(adapter);
                }
                break;
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        GotoOtherPage = false;
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
            Intent notifybackground3 = new Intent(DeviceStatusViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground3);
            Intent notifybackground4 = new Intent(ErrorStatusViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground4);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    /*
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
}
