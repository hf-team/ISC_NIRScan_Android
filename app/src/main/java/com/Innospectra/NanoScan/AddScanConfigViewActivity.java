package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ISCSDK.ISCNIRScanSDK;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ISCSDK.ISCNIRScanSDK.GetMaxPatternJNI;
import static com.ISCSDK.ISCNIRScanSDK.WriteScanConfiguration;

/**
 * Created by iris.lin on 2017/12/21.
 */


public class AddScanConfigViewActivity extends Activity {

    private byte []passSpectrumCalCoefficients = new byte[144];
    //-------------------------------------------------------------------------------------------------------------------------------
    private static Context mContext;
    ArrayList<ScrollView> scroll_section = new ArrayList<>();
    ScrollView layout_sections1;
    ScrollView layout_sections2;
    ScrollView layout_sections3;
    ScrollView layout_sections4;
    ScrollView layout_sections5;

    ArrayList<Button> btn_section = new ArrayList<>();
    Button btn_section1;
    Button btn_section2;
    Button btn_section3;
    Button btn_section4;
    Button btn_section5;

    ArrayList<EditText> et_type = new ArrayList<>();
    EditText et_type1;
    EditText et_type2;
    EditText et_type3;
    EditText et_type4;
    EditText et_type5;

    ArrayList<EditText> et_width = new ArrayList<>();
    EditText et_width1;
    EditText et_width2;
    EditText et_width3;
    EditText et_width4;
    EditText et_width5;
    ArrayList<TextView> tv_width = new ArrayList<>();
    TextView tv_width1;
    TextView tv_width2;
    TextView tv_width3;
    TextView tv_width4;
    TextView tv_width5;

    ArrayList<EditText> et_spec_start = new ArrayList<>();
    EditText et_spec_start1;
    EditText et_spec_start2;
    EditText et_spec_start3;
    EditText et_spec_start4;
    EditText et_spec_start5;

    ArrayList<EditText> et_spec_end = new ArrayList<>();
    EditText et_spec_end1;
    EditText et_spec_end2;
    EditText et_spec_end3;
    EditText et_spec_end4;
    EditText et_spec_end5;

    ArrayList<EditText> et_res = new ArrayList<>();
    EditText et_res1;
    EditText et_res2;
    EditText et_res3;
    EditText et_res4;
    EditText et_res5;

    ArrayList<EditText> et_exposure = new ArrayList<>();
    EditText et_exposure1;
    EditText et_exposure2;
    EditText et_exposure3;
    EditText et_exposure4;
    EditText et_exposure5;

    ArrayList<TextView>tv_exposure = new ArrayList<>();
    TextView tv_exposure1;
    TextView tv_exposure2;
    TextView tv_exposure3;
    TextView tv_exposure4;
    TextView tv_exposure5;

    ArrayList<TextView>tv_res = new ArrayList<>();
    TextView tv_res1;
    TextView tv_res2;
    TextView tv_res3;
    TextView tv_res4;
    TextView tv_res5;

    EditText et_config_index;
    EditText et_serial_num;
    EditText et_name;
    EditText et_repeat_add;
    EditText et_total_section;

    private static final int SECTION1 = 1;
    private static final int SECTION2 = 2;
    private static final int SECTION3 = 3;
    private static final int SECTION4 = 4;
    private static final int SECTION5 = 5;
    private int section=SECTION1;

    private Menu mMenu;

    private AlertDialog alertDialog;
    private int[] init_type = new int[5];
    private int[] init_width = new int[5];
    private int[] init_start_nm = new int[5];
    private int[] init_end_nm = new int[5];
    private int[] init_res = new int[5];
    private int[] init_exposure = new int[5];
    private String init_config_name = "";
    private  int init_scan_repeat = 1;
    private int init_section = 1;
    private ArrayList<String>text_width = new ArrayList<>();
    private ArrayList<String>text_exposure = new ArrayList<>();

    private Boolean saveConfig=false;
    int MaxPattern = 0;
    private ArrayList<String>ConfigName = new ArrayList<>();

    private final IntentFilter WriteScanConfigStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS);
    private final BroadcastReceiver WriteScanConfigStatusReceiver = new WriteScanConfigStatusReceiver();
    private ProgressBar calProgress;
    private int minWavelength=900;
    private int maxWavelength=1700;

    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_add_scan_config);
        mContext = this;
        if(ScanViewActivity.isExtendVer)
        {
            minWavelength = 1350;
            maxWavelength = 2150;
        }
        else
        {
            minWavelength = 900;
            maxWavelength = 1700;
        }
        //Set up the action bar title, and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.adding_configurations));
        }
        ConfigName = ScanConfigurationsViewActivity.ScanConfigName;
        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        init_EditText_config();
        init_ScrollView();
        init_Button_Section();
        init_EditText_Type();
        init_EditText_Width();
        init_text_width();
        init_EditText_Spec_Start();
        init_EditText_Spec_End();
        init_EditText_Res();
        init_EditText_Exposure();
        init_text_exposure();
        section = SECTION1;
        show_ScrollView();
        set_button_color();
        show_section_button();

        int Configindex = getIntent().getIntExtra("Store config size",0) +1;
        et_config_index.setText(Integer.toString(Configindex));
        String SerialNum = getIntent().getStringExtra("Serial Number");
        et_serial_num.setText(SerialNum);
        GetMaxPattern(0);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(WriteScanConfigStatusReceiver, WriteScanConfigStatusFilter);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
    }
    @Override
    public void onResume(){
        super.onResume();
        GotoOtherPage = false;
    }
    private void init_text_exposure()
    {
        text_exposure.add("Exposure(0-6): 0.635ms");
        text_exposure.add("Exposure(0-6): 1.27ms");
        text_exposure.add("Exposure(0-6): 2.54ms");
        text_exposure.add("Exposure(0-6): 5.08ms");
        text_exposure.add("Exposure(0-6): 15.24ms");
        text_exposure.add("Exposure(0-6): 30.48ms");
        text_exposure.add("Exposure(0-6): 60.96ms");
    }
    private void init_text_width()
    {
        text_width.add("Width(2-52):2.34nm");
        text_width.add("Width(2-52):3.51nm");
        text_width.add("Width(2-52):4.68nm");
        text_width.add("Width(2-52):5.85nm");
        text_width.add("Width(2-52):7.02nm");
        text_width.add("Width(2-52):8.19nm");
        text_width.add("Width(2-52):9.36nm");
        text_width.add("Width(2-52):10.53nm");
        text_width.add("Width(2-52):11.70nm");
        text_width.add("Width(2-52):12.87nm");
        text_width.add("Width(2-52):14.04nm");
        text_width.add("Width(2-52):15.21nm");
        text_width.add("Width(2-52):16.38nm");
        text_width.add("Width(2-52):17.55nm");
        text_width.add("Width(2-52):18.72nm");
        text_width.add("Width(2-52):19.89nm");
        text_width.add("Width(2-52):21.06nm");
        text_width.add("Width(2-52):22.23nm");
        text_width.add("Width(2-52):23.40nm");
        text_width.add("Width(2-52):24.57nm");
        text_width.add("Width(2-52):25.74nm");
        text_width.add("Width(2-52):26.91nm");
        text_width.add("Width(2-52):28.08nm");
        text_width.add("Width(2-52):29.25nm");
        text_width.add("Width(2-52):30.42nm");
        text_width.add("Width(2-52):31.59nm");
        text_width.add("Width(2-52):32.76nm");
        text_width.add("Width(2-52):33.93nm");
        text_width.add("Width(2-52):35.10nm");
        text_width.add("Width(2-52):36.27nm");
        text_width.add("Width(2-52):37.44nm");
        text_width.add("Width(2-52):38.61nm");
        text_width.add("Width(2-52):39.78nm");
        text_width.add("Width(2-52):40.95nm");
        text_width.add("Width(2-52):42.12nm");
        text_width.add("Width(2-52):43.29nm");
        text_width.add("Width(2-52):44.46nm");
        text_width.add("Width(2-52):45.63nm");
        text_width.add("Width(2-52):46.80nm");
        text_width.add("Width(2-52):47.97nm");
        text_width.add("Width(2-52):49.14nm");
        text_width.add("Width(2-52):50.31nm");
        text_width.add("Width(2-52):51.48nm");
        text_width.add("Width(2-52):52.65nm");
        text_width.add("Width(2-52):53.82nm");
        text_width.add("Width(2-52):54.99nm");
        text_width.add("Width(2-52):56.16nm");
        text_width.add("Width(2-52):57.33nm");
        text_width.add("Width(2-52):58.50nm");
        text_width.add("Width(2-52):59.67nm");
        text_width.add("Width(2-52):60.84nm");
    }
    private void init_EditText_config()
    {
        et_config_index = (EditText)findViewById(R.id.et_config_index);
        et_serial_num = (EditText)findViewById(R.id.et_serial_num);
        et_name = (EditText)findViewById(R.id.et_name);
        et_repeat_add = (EditText)findViewById(R.id.et_repeat_add);
        et_total_section = (EditText)findViewById(R.id.et_total_section);

        et_config_index.setEnabled(false);
        et_serial_num.setEnabled(false);

        SimpleDateFormat filesimpleDateFormat = new SimpleDateFormat("ddHHmmss", java.util.Locale.getDefault());
        String ConfigName = "BLE-Cfg-" + filesimpleDateFormat.format(new Date());
        et_name.setText(ConfigName);
        et_total_section.setOnEditorActionListener(total_section_listener);

        init_config_name = et_name.getText().toString();
        init_scan_repeat = Integer.parseInt(et_repeat_add.getText().toString());
        init_section = Integer.parseInt(et_total_section.getText().toString());
        et_name.setOnEditorActionListener(et_name_listener);
        et_repeat_add.setOnEditorActionListener(repeat_listener);
    }
    private void init_EditText_Type()
    {
        et_type1 = (EditText)findViewById(R.id.et_type1);
        et_type2 = (EditText)findViewById(R.id.et_type2);
        et_type3 = (EditText)findViewById(R.id.et_type3);
        et_type4 = (EditText)findViewById(R.id.et_type4);
        et_type5 = (EditText)findViewById(R.id.et_type5);

        et_type.add(et_type1);
        et_type.add(et_type2);
        et_type.add(et_type3);
        et_type.add(et_type4);
        et_type.add(et_type5);

        for(int i=0;i<et_type.size();i++)
        {

            init_type[i] = (Integer.parseInt(et_type.get(i).getText().toString()));
        }
        et_type.get(0).setOnEditorActionListener(section1_type);
        et_type.get(1).setOnEditorActionListener(section2_type);
        et_type.get(2).setOnEditorActionListener(section3_type);
        et_type.get(3).setOnEditorActionListener(section4_type);
        et_type.get(4).setOnEditorActionListener(section5_type);
    }
    private void init_EditText_Width()
    {
        tv_width1 = (TextView)findViewById(R.id.tv_width1);
        tv_width2 = (TextView)findViewById(R.id.tv_width2);
        tv_width3 = (TextView)findViewById(R.id.tv_width3);
        tv_width4 = (TextView)findViewById(R.id.tv_width4);
        tv_width5 = (TextView)findViewById(R.id.tv_width5);

        tv_width.add(tv_width1);
        tv_width.add(tv_width2);
        tv_width.add(tv_width3);
        tv_width.add(tv_width4);
        tv_width.add(tv_width5);

        et_width1 = (EditText)findViewById(R.id.et_width1);
        et_width2 = (EditText)findViewById(R.id.et_width2);
        et_width3 = (EditText)findViewById(R.id.et_width3);
        et_width4 = (EditText)findViewById(R.id.et_width4);
        et_width5 = (EditText)findViewById(R.id.et_width5);

        et_width.add(et_width1);
        et_width.add(et_width2);
        et_width.add(et_width3);
        et_width.add(et_width4);
        et_width.add(et_width5);

        for(int i=0;i<et_width.size();i++)
        {

            init_width[i] = (Integer.parseInt(et_width.get(i).getText().toString()));
        }
        et_width.get(0).setOnEditorActionListener(section1_width);
        et_width.get(1).setOnEditorActionListener(section2_width);
        et_width.get(2).setOnEditorActionListener(section3_width);
        et_width.get(3).setOnEditorActionListener(section4_width);
        et_width.get(4).setOnEditorActionListener(section5_width);
    }
    private void init_EditText_Spec_Start()
    {
        et_spec_start1 = (EditText)findViewById(R.id.et_spec_start1);
        et_spec_start2 = (EditText)findViewById(R.id.et_spec_start2);
        et_spec_start3 = (EditText)findViewById(R.id.et_spec_start3);
        et_spec_start4 = (EditText)findViewById(R.id.et_spec_start4);
        et_spec_start5 = (EditText)findViewById(R.id.et_spec_start5);

        et_spec_start.add(et_spec_start1);
        et_spec_start.add(et_spec_start2);
        et_spec_start.add(et_spec_start3);
        et_spec_start.add(et_spec_start4);
        et_spec_start.add(et_spec_start5);

        for(int i=0;i<et_spec_start.size();i++)
        {

            init_start_nm[i] = minWavelength;
            et_spec_start.get(i).setText(Integer.toString(minWavelength));
        }

        et_spec_start.get(0).setOnEditorActionListener(section1_spec_start);
        et_spec_start.get(1).setOnEditorActionListener(section2_spec_start);
        et_spec_start.get(2).setOnEditorActionListener(section3_spec_start);
        et_spec_start.get(3).setOnEditorActionListener(section4_spec_start);
        et_spec_start.get(4).setOnEditorActionListener(section5_spec_start);
    }
    private void init_EditText_Spec_End()
    {
        et_spec_end1 = (EditText)findViewById(R.id.et_spec_spec_end1);
        et_spec_end2 = (EditText)findViewById(R.id.et_spec_spec_end2);
        et_spec_end3 = (EditText)findViewById(R.id.et_spec_spec_end3);
        et_spec_end4 = (EditText)findViewById(R.id.et_spec_spec_end4);
        et_spec_end5 = (EditText)findViewById(R.id.et_spec_spec_end5);

        et_spec_end.add(et_spec_end1);
        et_spec_end.add(et_spec_end2);
        et_spec_end.add(et_spec_end3);
        et_spec_end.add(et_spec_end4);
        et_spec_end.add(et_spec_end5);

        for(int i=0;i<et_spec_end.size();i++)
        {

            init_end_nm[i] = maxWavelength;
            et_spec_end.get(i).setText(Integer.toString(maxWavelength));
        }

        et_spec_end.get(0).setOnEditorActionListener(section1_spec_end);
        et_spec_end.get(1).setOnEditorActionListener(section2_spec_end);
        et_spec_end.get(2).setOnEditorActionListener(section3_spec_end);
        et_spec_end.get(3).setOnEditorActionListener(section4_spec_end);
        et_spec_end.get(4).setOnEditorActionListener(section5_spec_end);
    }
    private void init_EditText_Res()
    {
        tv_res1 = (TextView)findViewById(R.id.tv_res1);
        tv_res2 = (TextView)findViewById(R.id.tv_res2);
        tv_res3 = (TextView)findViewById(R.id.tv_res3);
        tv_res4 = (TextView)findViewById(R.id.tv_res4);
        tv_res5 = (TextView)findViewById(R.id.tv_res5);

        tv_res.add(tv_res1);
        tv_res.add(tv_res2);
        tv_res.add(tv_res3);
        tv_res.add(tv_res4);
        tv_res.add(tv_res5);

        et_res1 = (EditText)findViewById(R.id.et_res1);
        et_res2 = (EditText)findViewById(R.id.et_res2);
        et_res3 = (EditText)findViewById(R.id.et_res3);
        et_res4 = (EditText)findViewById(R.id.et_res4);
        et_res5 = (EditText)findViewById(R.id.et_res5);

        et_res.add(et_res1);
        et_res.add(et_res2);
        et_res.add(et_res3);
        et_res.add(et_res4);
        et_res.add(et_res5);

        for(int i=0;i<et_res.size();i++)
        {

            init_res[i] = (Integer.parseInt(et_res.get(i).getText().toString()));
        }
        et_res.get(0).setOnEditorActionListener(section1_res);
        et_res.get(1).setOnEditorActionListener(section2_res);
        et_res.get(2).setOnEditorActionListener(section3_res);
        et_res.get(3).setOnEditorActionListener(section4_res);
        et_res.get(4).setOnEditorActionListener(section5_res);
    }
    private void init_EditText_Exposure()
    {
        tv_exposure1 = (TextView)findViewById(R.id.tv_exposure1);
        tv_exposure2 = (TextView)findViewById(R.id.tv_exposure2);
        tv_exposure3 = (TextView)findViewById(R.id.tv_exposure3);
        tv_exposure4 = (TextView)findViewById(R.id.tv_exposure4);
        tv_exposure5 = (TextView)findViewById(R.id.tv_exposure5);

        tv_exposure.add(tv_exposure1);
        tv_exposure.add(tv_exposure2);
        tv_exposure.add(tv_exposure3);
        tv_exposure.add(tv_exposure4);
        tv_exposure.add(tv_exposure5);

        et_exposure1 = (EditText)findViewById(R.id.et_exposure1);
        et_exposure2 = (EditText)findViewById(R.id.et_exposure2);
        et_exposure3 = (EditText)findViewById(R.id.et_exposure3);
        et_exposure4 = (EditText)findViewById(R.id.et_exposure4);
        et_exposure5 = (EditText)findViewById(R.id.et_exposure5);

        et_exposure.add(et_exposure1);
        et_exposure.add(et_exposure2);
        et_exposure.add(et_exposure3);
        et_exposure.add(et_exposure4);
        et_exposure.add(et_exposure5);

        for(int i=0;i<et_exposure.size();i++)
        {

            init_exposure[i] = (Integer.parseInt(et_exposure.get(i).getText().toString()));
        }

        et_exposure.get(0).setOnEditorActionListener(section1_exposure);
        et_exposure.get(1).setOnEditorActionListener(section2_exposure);
        et_exposure.get(2).setOnEditorActionListener(section3_exposure);
        et_exposure.get(3).setOnEditorActionListener(section4_exposure);
        et_exposure.get(4).setOnEditorActionListener(section5_exposure);
    }
    private void init_Button_Section()
    {
        btn_section1 = (Button) findViewById(R.id.btn_section1);
        btn_section2 = (Button) findViewById(R.id.btn_section2);
        btn_section3 = (Button) findViewById(R.id.btn_section3);
        btn_section4 = (Button) findViewById(R.id.btn_section4);
        btn_section5 = (Button) findViewById(R.id.btn_section5);

        btn_section1.setOnClickListener(btn_section1_listenser);
        btn_section2.setOnClickListener(btn_section2_listenser);
        btn_section3.setOnClickListener(btn_section3_listenser);
        btn_section4.setOnClickListener(btn_section4_listenser);
        btn_section5.setOnClickListener(btn_section5_listenser);

        btn_section.add(btn_section1);
        btn_section.add(btn_section2);
        btn_section.add(btn_section3);
        btn_section.add(btn_section4);
        btn_section.add(btn_section5);
    }
    private void set_button_color()
    {
        for(int i=0;i<5;i++)
        {
            if(i+1 == section)
            {
                btn_section.get(i).setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            }
            else
            {
                btn_section.get(i).setBackgroundColor(0xFF0099CC);
            }
        }
    }
    private void init_ScrollView()
    {
        layout_sections1 = (ScrollView) findViewById(R.id.layout_sections1);
        layout_sections2 = (ScrollView) findViewById(R.id.layout_sections2);
        layout_sections3 = (ScrollView) findViewById(R.id.layout_sections3);
        layout_sections4 = (ScrollView) findViewById(R.id.layout_sections4);
        layout_sections5 = (ScrollView) findViewById(R.id.layout_sections5);

        scroll_section.add(layout_sections1);
        scroll_section.add(layout_sections2);
        scroll_section.add(layout_sections3);
        scroll_section.add(layout_sections4);
        scroll_section.add(layout_sections5);
    }

    private void show_ScrollView()
    {
        for (int i=0;i<5;i++)
        {
            if(i+1 == section)
            {
                scroll_section.get(i).setVisibility(View.VISIBLE);
            }
            else
            {
                scroll_section.get(i).setVisibility(View.GONE);
            }
        }
    }

    private Button.OnClickListener btn_section1_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            section = SECTION1;
            set_button_color();
            show_ScrollView();
            GetMaxPattern(0);
            HideAllInputKeyboard();
        }
    };
    private Button.OnClickListener btn_section2_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            section = SECTION2;
            set_button_color();
            show_ScrollView();
            GetMaxPattern(1);
            HideAllInputKeyboard();
        }
    };
    private Button.OnClickListener btn_section3_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            section = SECTION3;
            set_button_color();
            show_ScrollView();
            GetMaxPattern(2);
            HideAllInputKeyboard();
        }
    };
    private Button.OnClickListener btn_section4_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            section = SECTION4;
            set_button_color();
            show_ScrollView();
            GetMaxPattern(3);
            HideAllInputKeyboard();
        }
    };
    private Button.OnClickListener btn_section5_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            section = SECTION5;
            set_button_color();
            show_ScrollView();
            GetMaxPattern(4);
            HideAllInputKeyboard();
        }
    };
    private void show_section_button()
    {
        for(int i=0;i<5;i++)
        {
            if(i+1<=Integer.parseInt(et_total_section.getText().toString()))
            {
                btn_section.get(i).setVisibility(View.VISIBLE);
            }
            else
            {
                btn_section.get(i).setVisibility(View.GONE);
            }
        }
    }
    //total section listener--------------------------------------------------------------------------
    private EditText.OnEditorActionListener total_section_listener = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_total_section.getText().toString().matches("")||Integer.parseInt(et_total_section.getText().toString())<1||Integer.parseInt(et_total_section.getText().toString())>5)
                {
                    et_total_section.setText(Integer.toString(init_section));
                    Dialog_Pane("Error","Scan sections needs to be between 1~5!");
                    return false; // consume.

                }
                else
                {
                    init_section = Integer.parseInt(et_total_section.getText().toString());
                    show_section_button();
                    section=SECTION1;
                    set_button_color();
                    show_ScrollView();
                    return false; // consume.
                }

            }
            return false;
        }
    };
    //total repeat listener--------------------------------------------------------------------------
    private EditText.OnEditorActionListener repeat_listener = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_repeat_add.getText().toString().matches("")||Integer.parseInt(et_repeat_add.getText().toString())<1||Integer.parseInt(et_repeat_add.getText().toString())>65535)
                {
                    et_repeat_add.setText(Integer.toString(init_scan_repeat));
                    Dialog_Pane("Error","Scan Repeats needs to be between 1~65535!");
                    return false; // consume.

                }

            }
            init_scan_repeat = Integer.parseInt(et_repeat_add.getText().toString());
            return false;
        }
    };
    //type   listener---------------------------------------------
    private EditText.OnEditorActionListener section1_type = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(0).getText().toString().matches("")|| Integer.parseInt(et_type.get(0).getText().toString())>1 || Integer.parseInt(et_type.get(0).getText().toString())<0)
                {
                    et_type.get(0).setText(Integer.toString(init_type[0]));
                    Dialog_Pane("Error","Scan type needs to be '0' or '1'!");
                    return false; // consume.

                }
            }
            init_type[0] = Integer.parseInt(et_type.get(0).getText().toString());
            GetMaxPattern(0);
            return false;
        }
    };


    private EditText.OnEditorActionListener section2_type = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(1).getText().toString().matches("")|| Integer.parseInt(et_type.get(1).getText().toString())>1 || Integer.parseInt(et_type.get(1).getText().toString())<0)
                {
                    et_type.get(1).setText(Integer.toString(init_type[1]));
                    Dialog_Pane("Error","Scan type needs to be '0' or '1'!");
                    return false; // consume.

                }
            }
            init_type[1] = Integer.parseInt(et_type.get(1).getText().toString());
            GetMaxPattern(1);
            return false;
        }
    };

    private EditText.OnEditorActionListener section3_type = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(2).getText().toString().matches("")|| Integer.parseInt(et_type.get(2).getText().toString())>1 || Integer.parseInt(et_type.get(2).getText().toString())<0)
                {
                    et_type.get(2).setText(Integer.toString(init_type[2]));
                    Dialog_Pane("Error","Scan type needs to be '0' or '1'!");
                    return false; // consume.

                }
            }
            init_type[2] = Integer.parseInt(et_type.get(2).getText().toString());
            GetMaxPattern(2);
            return false;
        }
    };

    private EditText.OnEditorActionListener section4_type = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(3).getText().toString().matches("")|| Integer.parseInt(et_type.get(3).getText().toString())>1 || Integer.parseInt(et_type.get(3).getText().toString())<0)
                {
                    et_type.get(3).setText(Integer.toString(init_type[3]));
                    Dialog_Pane("Error","Scan type needs to be '0' or '1'!");
                    return false; // consume.

                }
            }
            init_type[3] = Integer.parseInt(et_type.get(3).getText().toString());
            GetMaxPattern(3);
            return false;
        }
    };

    private EditText.OnEditorActionListener section5_type = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(4).getText().toString().matches("")|| Integer.parseInt(et_type.get(4).getText().toString())>1 || Integer.parseInt(et_type.get(4).getText().toString())<0)
                {
                    et_type.get(4).setText(Integer.toString(init_type[4]));
                    Dialog_Pane("Error","Scan type needs to be '0' or '1'!");
                    return false; // consume.

                }
            }
            init_type[4] = Integer.parseInt(et_type.get(4).getText().toString());
            GetMaxPattern(4);
            return false;
        }
    };
    //width listener------------------------------------------------------------------------------------------------------
    private EditText.OnEditorActionListener section1_width = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(0).getText().toString().matches("")|| Integer.parseInt(et_width.get(0).getText().toString())>52 || Integer.parseInt(et_width.get(0).getText().toString())<2)
                {
                    et_width.get(0).setText(Integer.toString(init_width[0]));
                    Dialog_Pane("Error","Scan width in pixels needs to be between '2' to '52'!");
                    return false; // consume.

                }
            }
            init_width[0] = Integer.parseInt(et_width.get(0).getText().toString());
            int index = init_width[0] -2;
            tv_width.get(0).setText(text_width.get(index));
            GetMaxPattern(0);
            return false;
        }
    };

    private EditText.OnEditorActionListener section2_width = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(1).getText().toString().matches("")|| Integer.parseInt(et_width.get(1).getText().toString())>52 || Integer.parseInt(et_width.get(1).getText().toString())<2)
                {
                    et_width.get(1).setText(Integer.toString(init_width[1]));
                    Dialog_Pane("Error","Scan width in pixels needs to be between '2' to '52'!");
                    return false; // consume.

                }
            }
            init_width[1] = Integer.parseInt(et_width.get(1).getText().toString());
            int index = init_width[1] -2;
            tv_width.get(1).setText(text_width.get(index));
            GetMaxPattern(1);
            return false;
        }
    };

    private EditText.OnEditorActionListener section3_width = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(2).getText().toString().matches("")|| Integer.parseInt(et_width.get(2).getText().toString())>52 || Integer.parseInt(et_width.get(2).getText().toString())<2)
                {
                    et_width.get(2).setText(Integer.toString(init_width[2]));
                    Dialog_Pane("Error","Scan width in pixels needs to be between '2' to '52'!");
                    return false; // consume.

                }
            }
            init_width[2] = Integer.parseInt(et_width.get(2).getText().toString());
            int index = init_width[2] -2;
            tv_width.get(2).setText(text_width.get(index));
            GetMaxPattern(2);
            return false;
        }
    };

    private EditText.OnEditorActionListener section4_width = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(3).getText().toString().matches("")|| Integer.parseInt(et_width.get(3).getText().toString())>52 || Integer.parseInt(et_width.get(3).getText().toString())<2)
                {
                    et_width.get(3).setText(Integer.toString(init_width[3]));
                    Dialog_Pane("Error","Scan width in pixels needs to be between '2' to '52'!");
                    return false; // consume.

                }
            }
            init_width[3] = Integer.parseInt(et_width.get(3).getText().toString());
            int index = init_width[3] -2;
            tv_width.get(3).setText(text_width.get(index));
            GetMaxPattern(3);
            return false;
        }
    };

    private EditText.OnEditorActionListener section5_width = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_type.get(4).getText().toString().matches("")|| Integer.parseInt(et_width.get(4).getText().toString())>52 || Integer.parseInt(et_width.get(4).getText().toString())<2)
                {
                    et_width.get(4).setText(Integer.toString(init_width[4]));
                    Dialog_Pane("Error","Scan width in pixels needs to be between '2' to '52'!");
                    return false; // consume.

                }
            }
            init_width[4] = Integer.parseInt(et_width.get(4).getText().toString());
            int index = init_width[4] -2;
            tv_width.get(4).setText(text_width.get(index));
            GetMaxPattern(4);
            return false;
        }
    };

    //spec start listener----------------------------------------------------------------------------------------------
    private EditText.OnEditorActionListener section1_spec_start = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_start.get(0).getText().toString().matches("")|| Integer.parseInt(et_spec_start.get(0).getText().toString())> Integer.parseInt(et_spec_end.get(0).getText().toString()) || Integer.parseInt(et_spec_start.get(0).getText().toString())<minWavelength)
                {
                    et_spec_start.get(0).setText(Integer.toString(init_start_nm[0]));
                    Dialog_Pane("Error","Start wavelength should be between " + minWavelength + "nm and end wavelength!");
                    return false; // consume.

                }
            }
            init_start_nm[0] = Integer.parseInt(et_spec_start.get(0).getText().toString());
            GetMaxPattern(0);
            return false;
        }
    };

    private EditText.OnEditorActionListener section2_spec_start = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_start.get(1).getText().toString().matches("")|| Integer.parseInt(et_spec_start.get(1).getText().toString())> Integer.parseInt(et_spec_end.get(1).getText().toString()) || Integer.parseInt(et_spec_start.get(1).getText().toString())<minWavelength)
                {
                    et_spec_start.get(1).setText(Integer.toString(init_start_nm[1]));
                    Dialog_Pane("Error","Start wavelength should be between " + minWavelength + "nm and end wavelength!");
                    return false; // consume.

                }
            }
            init_start_nm[1] = Integer.parseInt(et_spec_start.get(1).getText().toString());
            GetMaxPattern(1);
            return false;
        }
    };

    private EditText.OnEditorActionListener section3_spec_start = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_start.get(2).getText().toString().matches("")|| Integer.parseInt(et_spec_start.get(2).getText().toString())> Integer.parseInt(et_spec_end.get(2).getText().toString()) || Integer.parseInt(et_spec_start.get(2).getText().toString())<minWavelength)
                {
                    et_spec_start.get(2).setText(Integer.toString(init_start_nm[2]));
                    Dialog_Pane("Error","Start wavelength should be between " + minWavelength + "nm and end wavelength!");
                    return false; // consume.

                }
            }
            init_start_nm[2] = Integer.parseInt(et_spec_start.get(2).getText().toString());
            GetMaxPattern(2);
            return false;
        }
    };

    private EditText.OnEditorActionListener section4_spec_start = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_start.get(3).getText().toString().matches("")|| Integer.parseInt(et_spec_start.get(3).getText().toString())> Integer.parseInt(et_spec_end.get(3).getText().toString()) || Integer.parseInt(et_spec_start.get(3).getText().toString())<minWavelength)
                {
                    et_spec_start.get(3).setText(Integer.toString(init_start_nm[3]));
                    Dialog_Pane("Error","Start wavelength should be between " + minWavelength + "nm and end wavelength!");
                    return false; // consume.

                }
            }
            init_start_nm[3] = Integer.parseInt(et_spec_start.get(3).getText().toString());
            GetMaxPattern(3);
            return false;
        }
    };

    private EditText.OnEditorActionListener section5_spec_start = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_start.get(4).getText().toString().matches("")|| Integer.parseInt(et_spec_start.get(4).getText().toString())> Integer.parseInt(et_spec_end.get(4).getText().toString()) || Integer.parseInt(et_spec_start.get(4).getText().toString())<minWavelength)
                {
                    et_spec_start.get(4).setText(Integer.toString(init_start_nm[4]));
                    Dialog_Pane("Error","Start wavelength should be between " + minWavelength + "nm and end wavelength!");
                    return false; // consume.

                }
            }
            init_start_nm[4] = Integer.parseInt(et_spec_start.get(4).getText().toString());
            GetMaxPattern(4);
            return false;
        }
    };
    //spec end listener-------------------------------------------------------------------------------------------------------------
    private EditText.OnEditorActionListener section1_spec_end = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_end.get(0).getText().toString().matches("")|| Integer.parseInt(et_spec_end.get(0).getText().toString())< Integer.parseInt(et_spec_start.get(0).getText().toString()) || Integer.parseInt(et_spec_end.get(0).getText().toString())>maxWavelength)
                {
                    et_spec_end.get(0).setText(Integer.toString(init_end_nm[0]));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + maxWavelength + "nm!");
                    return false; // consume.

                }
            }
            init_end_nm[0] = Integer.parseInt(et_spec_end.get(0).getText().toString());
            GetMaxPattern(0);
            return false;
        }
    };

    private EditText.OnEditorActionListener section2_spec_end = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_end.get(1).getText().toString().matches("")|| Integer.parseInt(et_spec_end.get(1).getText().toString())< Integer.parseInt(et_spec_start.get(1).getText().toString()) || Integer.parseInt(et_spec_end.get(1).getText().toString())>maxWavelength)
                {
                    et_spec_end.get(1).setText(Integer.toString(init_end_nm[1]));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + maxWavelength +"nm!");
                    return false; // consume.

                }
            }
            init_end_nm[1] = Integer.parseInt(et_spec_end.get(1).getText().toString());
            GetMaxPattern(1);
            return false;
        }
    };
    private EditText.OnEditorActionListener section3_spec_end = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_end.get(2).getText().toString().matches("")|| Integer.parseInt(et_spec_end.get(2).getText().toString())< Integer.parseInt(et_spec_start.get(2).getText().toString()) || Integer.parseInt(et_spec_end.get(2).getText().toString())>maxWavelength)
                {
                    et_spec_end.get(2).setText(Integer.toString(init_end_nm[2]));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + maxWavelength + "nm!");
                    return false; // consume.

                }
            }
            init_end_nm[2] = Integer.parseInt(et_spec_end.get(2).getText().toString());
            GetMaxPattern(2);
            return false;
        }
    };
    private EditText.OnEditorActionListener section4_spec_end = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_end.get(3).getText().toString().matches("")|| Integer.parseInt(et_spec_end.get(3).getText().toString())< Integer.parseInt(et_spec_start.get(3).getText().toString()) || Integer.parseInt(et_spec_end.get(3).getText().toString())>maxWavelength)
                {
                    et_spec_end.get(3).setText(Integer.toString(init_end_nm[3]));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + maxWavelength + "nm!");
                    return false; // consume.

                }
            }
            init_end_nm[3] = Integer.parseInt(et_spec_end.get(3).getText().toString());
            GetMaxPattern(3);
            return false;
        }
    };
    private EditText.OnEditorActionListener section5_spec_end = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_spec_end.get(4).getText().toString().matches("")|| Integer.parseInt(et_spec_end.get(4).getText().toString())< Integer.parseInt(et_spec_start.get(4).getText().toString()) || Integer.parseInt(et_spec_end.get(4).getText().toString())>maxWavelength)
                {
                    et_spec_end.get(4).setText(Integer.toString(init_end_nm[4]));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + maxWavelength + "nm!");
                    return false; // consume.

                }
            }
            init_end_nm[4] = Integer.parseInt(et_spec_end.get(4).getText().toString());
            GetMaxPattern(4);
            return false;
        }
    };
    //exposure listener---------------------------------------------------------------------------------------------------------
    private EditText.OnEditorActionListener section1_exposure = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_exposure.get(0).getText().toString().matches("")|| Integer.parseInt(et_exposure.get(0).getText().toString())>6 || Integer.parseInt(et_exposure.get(0).getText().toString())<0)
                {
                    et_exposure.get(0).setText(Integer.toString(init_exposure[0]));
                    Dialog_Pane("Error","Scan exposure time index needs to be between '0' to '6'!");
                    return false; // consume.

                }
            }
            init_exposure[0] = Integer.parseInt(et_exposure.get(0).getText().toString());
            int index = init_exposure[0];
            tv_exposure.get(0).setText(text_exposure.get(index));
            return false;
        }
    };

    private EditText.OnEditorActionListener section2_exposure = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_exposure.get(1).getText().toString().matches("")|| Integer.parseInt(et_exposure.get(1).getText().toString())>6 || Integer.parseInt(et_exposure.get(1).getText().toString())<0)
                {
                    et_exposure.get(1).setText(Integer.toString(init_exposure[1]));
                    Dialog_Pane("Error","Scan exposure time index needs to be between '0' to '6'!");
                    return false; // consume.

                }
            }
            init_exposure[1] = Integer.parseInt(et_exposure.get(1).getText().toString());
            int index = init_exposure[1];
            tv_exposure.get(1).setText(text_exposure.get(index));
            return false;
        }
    };

    private EditText.OnEditorActionListener section3_exposure = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_exposure.get(2).getText().toString().matches("")|| Integer.parseInt(et_exposure.get(2).getText().toString())>6 || Integer.parseInt(et_exposure.get(2).getText().toString())<0)
                {
                    et_exposure.get(2).setText(Integer.toString(init_exposure[2]));
                    Dialog_Pane("Error","Scan exposure time index needs to be between '0' to '6'!");
                    return false; // consume.

                }
            }
            init_exposure[2] = Integer.parseInt(et_exposure.get(2).getText().toString());
            int index = init_exposure[2];
            tv_exposure.get(2).setText(text_exposure.get(index));
            return false;
        }
    };

    private EditText.OnEditorActionListener section4_exposure = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_exposure.get(3).getText().toString().matches("")|| Integer.parseInt(et_exposure.get(3).getText().toString())>6 || Integer.parseInt(et_exposure.get(3).getText().toString())<0)
                {
                    et_exposure.get(3).setText(Integer.toString(init_exposure[3]));
                    Dialog_Pane("Error","Scan exposure time index needs to be between '0' to '6'!");
                    return false; // consume.

                }
            }
            init_exposure[3] = Integer.parseInt(et_exposure.get(3).getText().toString());
            int index = init_exposure[3];
            tv_exposure.get(3).setText(text_exposure.get(index));
            return false;
        }
    };

    private EditText.OnEditorActionListener et_name_listener = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_name.getText().toString().length()<1||et_name.getText().toString().length()>19)
                {
                    et_name.setText(init_config_name);
                    Dialog_Pane("Error","Config name length should be 1~19!");
                    return false; // consume.

                }
            }
            init_config_name = et_name.getText().toString();
            return false;
        }
    };

    //config name listener---------------------------------------------------------------
    private EditText.OnEditorActionListener section5_exposure = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_exposure.get(4).getText().toString().matches("")|| Integer.parseInt(et_exposure.get(4).getText().toString())>6 || Integer.parseInt(et_exposure.get(4).getText().toString())<0)
                {
                    et_exposure.get(4).setText(Integer.toString(init_exposure[4]));
                    Dialog_Pane("Error","Scan exposure time index needs to be between '0' to '6'!");
                    return false; // consume.

                }
            }
            init_exposure[4] = Integer.parseInt(et_exposure.get(4).getText().toString());
            int index = init_exposure[4];
            tv_exposure.get(4).setText(text_exposure.get(index));
            return false;
        }
    };
    //res  ----------------------------------------------------------------------------------------------
    private EditText.OnEditorActionListener section1_res = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_res.get(0).getText().toString().matches("")|| Integer.parseInt(et_res.get(0).getText().toString())< 2 || Integer.parseInt(et_res.get(0).getText().toString())>MaxPattern)
                {
                    et_res.get(0).setText(Integer.toString(init_res[0]));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + "!");
                    return false; // consume.

                }
                int res = Getres();
                if(res<0)
                {
                    int shift  = Integer.parseInt(et_res.get(0).getText().toString()) + res;
                    init_res[0] = shift;
                    et_res.get(0).setText(Integer.toString(shift));
                    Dialog_Pane("Error","Total scan pattern num is over 624! \n Left pattern number is " +shift +".");
                    return false; // consume.
                }
            }
            init_res[0] = Integer.parseInt(et_res.get(0).getText().toString());
            return false;
        }
    };

    private EditText.OnEditorActionListener section2_res = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_res.get(1).getText().toString().matches("")|| Integer.parseInt(et_res.get(1).getText().toString())< 2 || Integer.parseInt(et_res.get(1).getText().toString())>MaxPattern)
                {
                    et_res.get(1).setText(Integer.toString(init_res[1]));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + "!");
                    return false; // consume.

                }
                int res = Getres();
                if(res<0)
                {
                    int shift  = Integer.parseInt(et_res.get(1).getText().toString()) + res;
                    init_res[1] = shift;
                    et_res.get(1).setText(Integer.toString(shift));
                    Dialog_Pane("Error","Total scan pattern num is over 624! \n Left pattern number is " +shift +".");
                    return false; // consume.
                }
            }
            init_res[1] = Integer.parseInt(et_res.get(1).getText().toString());
            return false;
        }
    };

    private EditText.OnEditorActionListener section3_res = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_res.get(2).getText().toString().matches("")|| Integer.parseInt(et_res.get(2).getText().toString())< 2 || Integer.parseInt(et_res.get(2).getText().toString())>MaxPattern)
                {
                    et_res.get(2).setText(Integer.toString(init_res[2]));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + "!");
                    return false; // consume.

                }
                int res = Getres();
                if(res<0)
                {
                    int shift  = Integer.parseInt(et_res.get(2).getText().toString()) + res;
                    init_res[2] = shift;
                    et_res.get(2).setText(Integer.toString(shift));
                    Dialog_Pane("Error","Total scan pattern num is over 624! \n Left pattern number is " +shift +".");
                    return false; // consume.
                }
            }
            init_res[2] = Integer.parseInt(et_res.get(2).getText().toString());
            return false;
        }
    };

    private EditText.OnEditorActionListener section4_res = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_res.get(3).getText().toString().matches("")|| Integer.parseInt(et_res.get(3).getText().toString())< 2 || Integer.parseInt(et_res.get(3).getText().toString())>MaxPattern)
                {
                    et_res.get(3).setText(Integer.toString(init_res[3]));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + "!");
                    return false; // consume.

                }
                int res = Getres();
                if(res<0)
                {
                    int shift  = Integer.parseInt(et_res.get(3).getText().toString()) + res;
                    init_res[3] = shift;
                    et_res.get(3).setText(Integer.toString(shift));
                    Dialog_Pane("Error","Total scan pattern num is over 624! \n Left pattern number is " +shift +".");
                    return false; // consume.
                }
            }
            init_res[3] = Integer.parseInt(et_res.get(3).getText().toString());
            return false;
        }
    };

    private EditText.OnEditorActionListener section5_res = new EditText.OnEditorActionListener()
    {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_res.get(4).getText().toString().matches("")|| Integer.parseInt(et_res.get(4).getText().toString())< 2 || Integer.parseInt(et_res.get(4).getText().toString())>MaxPattern)
                {
                    et_res.get(4).setText(Integer.toString(init_res[4]));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + "!");
                    return false; // consume.

                }
                int res = Getres();
                if(res<0)
                {
                    int shift  = Integer.parseInt(et_res.get(4).getText().toString()) + res;
                    init_res[4] = shift;
                    et_res.get(4).setText(Integer.toString(shift));
                    Dialog_Pane("Error","Total scan pattern num is over 624! \n Left pattern number is " +shift +".");
                    return false; // consume.
                }
            }
            init_res[4] = Integer.parseInt(et_res.get(4).getText().toString());
            return false;
        }
    };

    private int Getres()
    {
        int totalsection = Integer.parseInt(et_total_section.getText().toString());
        int res = 624;
        for(int i=0;i<totalsection;i++)
        {
            res-=Integer.parseInt(et_res.get(i).getText().toString());
        }
        return res;
    }

    private void Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void Save_Complete_Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                GotoOtherPage = true;
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Save_Fail_Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /*
    * Inflate the options menu
    * In this case, there is no menu and only an up indicator,
    * so the function should always return true.
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save_configuration, menu);
        mMenu = menu;
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            if(checkSaveConfigValue())
            {
                mMenu.findItem(R.id.action_save).setEnabled(false);
                saveConfig = true;
                ScanConfigurationsViewActivity.saveConfig = saveConfig;//notify should refresh scan config list
                calProgress.setVisibility(View.VISIBLE);
                byte[]EXTRA_DATA = ChangeScanConfigToByte();
                ISCNIRScanSDK.ScanConfig(EXTRA_DATA,ISCNIRScanSDK.ScanConfig.SAVE);
            }
        }
        if (id == android.R.id.home) {
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
            Intent notifybackground3 = new Intent(ScanConfigurationsViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground3);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    private Boolean checkSaveConfigValue()
    {
        //check duplicate config name---------------------------------------------------------------------------------------
        for(int i=0;i< ConfigName.size();i++)
        {
            if(ConfigName.get(i).equals(et_name.getText().toString()))
            {
                Dialog_Pane("Error","Duplicate Config Name.");
                return false;
            }
        }
        //check res---------------------------------------------------------------------------------
        int section = Integer.parseInt(et_total_section.getText().toString());
        int total =0;
        for(int i=0;i<section;i++)
        {
            total += Integer.parseInt(et_res.get(i).getText().toString());
        }
        if(total>624)
        {
            Dialog_Pane("Error","Total scan pattern number is over 624!");
            return false;
        }
        return true;
    }

    /**
     * Custom receiver for returning the event that reference calibrations have been read(SaveScanConfig should be called)
     */
    public class WriteScanConfigStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.GONE);
            mMenu.findItem(R.id.action_save).setEnabled(true);
            byte status[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS);
            if((int)status[0] == 1)
            {
                if((int)status[1] == 1)
                {
                    Save_Complete_Dialog_Pane("Configuration Saving","Configuration has been saved to device!");
                }
                else
                {
                    Save_Fail_Dialog_Pane("Fail","Save configuration fail!");
                }
            }
            else if((int)status[0] == -1)
            {
                Save_Fail_Dialog_Pane("Fail","Save configuration fail!");
            }
            else if((int)status[0] == -2)
            {
                Save_Fail_Dialog_Pane("Fail","Save configuration fail! Hardware not compatible!");
            }
            else if((int)status[0] == -3)
            {
                Save_Fail_Dialog_Pane("Fail","Save configuration fail! Function is currently locked!" );
            }
        }
    }
    private void HideAllInputKeyboard()
    {
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_type.get(i).getWindowToken(), 0);
        }
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_width.get(i).getWindowToken(), 0);
        }
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_spec_start.get(i).getWindowToken(), 0);
        }
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_spec_end.get(i).getWindowToken(), 0);
        }
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_res.get(i).getWindowToken(), 0);
        }
        for(int i=0;i<section;i++)
        {
            imm.hideSoftInputFromWindow(et_exposure.get(i).getWindowToken(), 0);
        }
    }

    /**
     * Transmit scan config data to the byte
     */
    public byte[] ChangeScanConfigToByte()
    {
        ISCNIRScanSDK.ScanConfigInfo write_scan_config = new ISCNIRScanSDK.ScanConfigInfo();
        //transfer config name to byte
        String isoString = et_name.getText().toString();
        int name_size = isoString.length();
        byte[] ConfigNamebytes=isoString.getBytes();
        for(int i=0;i<=name_size;i++)
        {
            if(i == name_size)
                write_scan_config.configName[i] = 0;
            else
                write_scan_config.configName[i] = ConfigNamebytes[i];
        }
        write_scan_config.write_scanType = 2;
        //transfer SerialNumber to byte
        String SerialNumber = et_serial_num.getText().toString();
        byte[] SerialNumberbytes=SerialNumber.getBytes();
        int SerialNumber_size = SerialNumber.length();
        for(int i=0;i<SerialNumber_size;i++)
        {
            write_scan_config.scanConfigSerialNumber[i] = SerialNumberbytes[i];
        }
        write_scan_config.write_scanConfigIndex = 255;
        write_scan_config.write_numSections =(byte) Integer.parseInt(et_total_section.getText().toString());
        write_scan_config.write_numRepeat = Integer.parseInt(et_repeat_add.getText().toString());
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionScanType[i] = (byte)Integer.parseInt(et_type.get(i).getText().toString());
        }
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionWavelengthStartNm[i] =Integer.parseInt(et_spec_start.get(i).getText().toString());
        }
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionWavelengthEndNm[i] =Integer.parseInt(et_spec_end.get(i).getText().toString());
        }
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionNumPatterns[i] =Integer.parseInt(et_res.get(i).getText().toString());
        }
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionWidthPx[i] = (byte)Integer.parseInt(et_width.get(i).getText().toString());
        }
        for(int i=0;i<5;i++)
        {
            write_scan_config.sectionExposureTime[i] =Integer.parseInt(et_exposure.get(i).getText().toString());
        }
        //transmit scan config data to the byte
        return  ISCNIRScanSDK.WriteScanConfiguration(write_scan_config);
    }
    /**
     *Get max pattern that user can set
     */
    private void GetMaxPattern(int i)
    {
        passSpectrumCalCoefficients = ScanViewActivity.passSpectrumCalCoefficients;
        int start_nm = Integer.parseInt(et_spec_start.get(i).getText().toString());
        int end_nm =  Integer.parseInt(et_spec_end.get(i).getText().toString());
        int width_index = Integer.parseInt(et_width.get(i).getText().toString());
        int num_repeat = Integer.parseInt(et_repeat_add.getText().toString());
        int scan_type = Integer.parseInt(et_type.get(i).getText().toString());
        int isEXTVer = 0;
        if(ScanViewActivity.isExtendVer)
        {
            isEXTVer = 1;
        }
        MaxPattern = ISCNIRScanSDK.GetMaxPatternJNI(scan_type,start_nm,end_nm,width_index,num_repeat,passSpectrumCalCoefficients,isEXTVer);
        String text = "D-Res. (pts, max:" + MaxPattern +")";
        tv_res.get(i).setText(text);
    }
}
