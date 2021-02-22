package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.ISCSDK.ISCNIRScanSDK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by iris.lin on 2019/8/20.
 */

public class ErrorStatusViewActivity extends Activity {
    private ListView listView;
    private static Context mContext;
    byte[] bufByteError;
    private Button btn_clear_error;

    int[] images = { R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
            R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray};
    String[] title = { "Scan", "ADC", "EEPROM", "Bluetooth", "Spectrum Library", "Hardware","TMP006" ,"HDC1000","Battery","Memory","UART","System"};
    private static Boolean GotoOtherPage = false;
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ErrorStatus.notifybackground";
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_status);
        mContext = this;
        //Set up the action bar title and enable the back arrow
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.detail_error_status));
        }
        btn_clear_error = (Button)findViewById(R.id.btn_clear_error);
        btn_clear_error.setOnClickListener(clear_error_listenser);
        Bundle bundle = getIntent().getExtras();
        String errstatus = bundle.getString("ERRSTATUS" );
        Log.d("Error Status", "Error Status:" + errstatus);
        byte[] errbyte = bundle.getByteArray("ERRBYTE");
        bufByteError  = errbyte;

        int[] images = { R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray};

        int data = errbyte[0]&0xFF | (errbyte[1] << 8);//0XFF avoid nagtive number

        int error_scan = 0x00000001;
        for(int j=0;j<2;j++)
        {
            int ret = data & error_scan;
            if(ret == error_scan)
            {
                images[j] = R.drawable.led_r;
            }
            error_scan = error_scan<<1;
        }
        error_scan = error_scan<<1;

        for(int j=2;j<12;j++)
        {
            int ret = data & error_scan;
            if(ret == error_scan)
            {
                images[j] = R.drawable.led_r;
            }
            error_scan = error_scan<<1;
        }
        listView = (ListView) findViewById(R.id.error_status_listview);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < images.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("images", images[i]);
            map.put("title", title[i]);
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.activity_error_status_item, new String[] { "images", "title" }, new int[] {
                R.id.image, R.id.error_test });
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onClickListView);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }

    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            GotoOtherPage = true;
            switch (position)
            {
                case 0://Scan
                    Intent graphIntent = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent.putExtra("POS",position);
                    graphIntent.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent);
                    break;
                case 1://ADC
                    Intent graphIntent1 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent1.putExtra("POS",position);
                    graphIntent1.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent1);
                    break;
                case 5://Hardware
                    Intent graphIntent5 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent5.putExtra("POS",position);
                    graphIntent5.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent5);
                    break;
                case 6://TMP006
                    Intent graphIntent6 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent6.putExtra("POS",position);
                    graphIntent6.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent6);
                    break;
                case 7://HDC1000
                    Intent graphIntent7 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent7.putExtra("POS",position);
                    graphIntent7.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent7);
                    break;
                case 8://Battery
                    Intent graphIntent8 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent8.putExtra("POS",position);
                    graphIntent8.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent8);
                    break;
                case 11://System
                    Intent graphIntent11 = new Intent(mContext, AdvanceErrorStatusViewActivity.class);
                    graphIntent11.putExtra("POS",position);
                    graphIntent11.putExtra("ERRBYTE",bufByteError);
                    startActivity(graphIntent11);
                    break;
            }
        }

    };
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

    private Button.OnClickListener clear_error_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            ISCNIRScanSDK.ClearDeviceError();
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < images.length; i++) {
                images[i] = R.drawable.leg_gray;
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("images", images[i]);
                map.put("title", title[i]);
                list.add(map);
            }
            SimpleAdapter adapter = new SimpleAdapter(mContext, list,
                    R.layout.activity_error_status_item, new String[] { "images", "title" }, new int[] {
                    R.id.image, R.id.error_test });
            listView.setAdapter(adapter);
            for(int i=0;i<bufByteError.length;i++)
            {
                bufByteError[i] = 0x00;
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    GotoOtherPage = false;
                    finish();
                }}, 300);
        }
    };
}
