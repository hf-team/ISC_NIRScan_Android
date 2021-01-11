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
 * Created by iris.lin on 2019/8/19.
 */

public class AdvanceDeviceStatusViewActivity extends Activity {
    private ListView listView;
    private static Boolean GotoOtherPage = false;
    Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advance_device_status);
        mContext = this;
        Bundle bundle = getIntent().getExtras();
        byte[] devbyte = bundle.getByteArray("DEVBYTE");
        int data = devbyte[0] | (devbyte[1] << 8);
        int tiva = 0x00000001;
        int[] images = { R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};

        for(int j=0;j<2;j++)
        {
            int ret = data & tiva;
            if(ret == tiva)
            {
                images[j] = R.drawable.led_g;
            }
            tiva = tiva<<1;
        }
        tiva = tiva<<2;
        for(int j=2;j<7;j++)
        {
            int ret = data & tiva;
            if(ret == tiva)
            {
                images[j] = R.drawable.led_g;
            }
            tiva = tiva<<1;
        }
        //Set up the action bar title and enable the back arrow
        ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.detail_device_status));
        }

        listView = (ListView) findViewById(R.id.device_status_listview);
        String[] title = { "Tiva", "Scanning", "BLE stack", "BLE connection", "Scan Data Interpreting", "Scan Button Pressed", "Battery in charge"};
        images[0] = R.drawable.led_g;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        //Put pictures and text into a collection
        for (int i = 0; i < images.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("images", images[i]);
            map.put("title", title[i]);
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.advance_device_status_item, new String[] { "images", "title" }, new int[] {
                R.id.image, R.id.text });
        listView.setAdapter(adapter);
    }
    @Override
    public void onResume(){
        super.onResume();
        GotoOtherPage = false;
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
}
