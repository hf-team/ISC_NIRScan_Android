package com.Innospectra.NanoScan;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ISCSDK.ISCNIRScanSDK;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.ISCSDK.ISCNIRScanSDK.Interpret_intensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_length;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_uncalibratedIntensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_wavelength;
import static com.ISCSDK.ISCNIRScanSDK.Reference_Info;
import static com.ISCSDK.ISCNIRScanSDK.Scan_Config_Info;
import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;
import static com.Innospectra.NanoScan.DeviceStatusViewActivity.GetLampTimeString;


/**
 * Activity controlling the Nano once it is connected
 * This activity allows a user to initiate a scan, as well as access other "connection-only"
 * settings. When first launched, the app will scan for a preferred device
 * for {@link com.ISCSDK.ISCNIRScanSDK#SCAN_PERIOD}, if it is not found, then it will start another "open"
 * scan for any Nano.
 *
 * If a preferred Nano has not been set, it will start a single scan. If at the end of scanning, a
 * Nano has not been found, a message will be presented to the user indicating and error, and the
 * activity will finish
 *
 * WARNING: This activity uses JNI function calls for communicating with the Spectrum C library, It
 * is important that the name and file structure of this activity remain unchanged, or the functions
 * will NOT work
 *
 * @author collinmast
 */


public class ScanViewActivity extends Activity {
    //20210207 zhaozz: 增加保存预测返回值的全局变量
    private String predictRetVal;
    //20210207 zhaozz：增加保存每次扫描absorbance的值的全局变量
    private List<Double> absorbanceList = new ArrayList<Double>();
    //region parameter
    private static Context mContext;
    private ProgressDialog barProgressDialog;
    private ProgressBar calProgress;
    private TextView progressBarinsideText;
    private AlertDialog alertDialog;
    private Menu mMenu;





//    public class NoScrollViewPager extends ViewPager {
//        public NoScrollViewPager(Context context, AttributeSet attrs) {
//            super(context, attrs);
//        }
//
//        public NoScrollViewPager(Context context) {
//            super(context);
//        }
//
//        // 20210224 zhaozz: 增加以用于禁止左右滑动
//        @Override
//        public boolean onInterceptTouchEvent(MotionEvent ev) {
//            return false;
//        }
//
//        @Override
//        public boolean onTouchEvent(MotionEvent ev) {
//            return false;
//        }
//    }

    private NoScrollViewPager mViewPager;
    private String GraphLabel = "智农宝";
    private ArrayList<String> mXValues;
    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Entry> mReferenceFloat;
    private ArrayList<Float> mWavelengthFloat;

    //! Tiva version is extend wavelength version or not
    public static Boolean isExtendVer = false;
    //! Control FW level to implement function
    public static FW_LEVEL fw_level  = FW_LEVEL.LEVEL_0;

    public enum FW_LEVEL
    {
        LEVEL_0, // Tiva < 2.1.0.67
        LEVEL_1, // Tiva >=2.1.0.67
        LEVEL_2, // Tiva >=2.4.3
        LEVEL_3, // Tiva >=2.4.3 and main board = "F"
        LEVEL_EXT_1, // Tiva >= 3.3.0
        LEVEL_EXT_2, // Tiva >= 3.3.0 and  main board = "0"
    }
    public enum ScanMethod {
        Normal, QuickSet, Manual,Maintain
    }
    public enum LampState{
        ON,OFF,AUTO
    }
    public enum PhysicalButton{
        Unlock,Lock
    }
    //endregion

    //region broadcast parameter
    private final BroadcastReceiver ScanDataReadyReceiver = new ScanDataReadyReceiver();
    private final BroadcastReceiver RefDataReadyReceiver = new RefDataReadyReceiver();
    private final BroadcastReceiver NotifyCompleteReceiver = new NotifyCompleteReceiver();
    private final BroadcastReceiver ScanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver RefCoeffDataProgressReceiver = new RefCoeffDataProgressReceiver();
    private final BroadcastReceiver CalMatrixDataProgressReceiver = new CalMatrixDataProgressReceiver();
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver SpectrumCalCoefficientsReadyReceiver = new SpectrumCalCoefficientsReadyReceiver();
    private final BroadcastReceiver RetrunReadActivateStatusReceiver = new RetrunReadActivateStatusReceiver();
    private final BroadcastReceiver RetrunActivateStatusReceiver = new RetrunActivateStatusReceiver();
    private final BroadcastReceiver ReturnCurrentScanConfigurationDataReceiver = new ReturnCurrentScanConfigurationDataReceiver();
    private final BroadcastReceiver DeviceInfoReceiver = new DeviceInfoReceiver();
    private final BroadcastReceiver GetUUIDReceiver = new GetUUIDReceiver();
    private final BroadcastReceiver GetDeviceStatusReceiver = new GetDeviceStatusReceiver();
    private final BroadcastReceiver ScanConfReceiver = new ScanConfReceiver();
    private final BroadcastReceiver WriteScanConfigStatusReceiver = new WriteScanConfigStatusReceiver();
    private final BroadcastReceiver ScanConfSizeReceiver=  new ScanConfSizeReceiver();
    private final BroadcastReceiver GetActiveScanConfReceiver = new GetActiveScanConfReceiver();
    private final BroadcastReceiver ReturnLampRampUpADCReceiver = new ReturnLampRampUpADCReceiver();
    private final BroadcastReceiver ReturnLampADCAverageReceiver = new ReturnLampADCAverageReceiver();
    private final BroadcastReceiver ReturnMFGNumReceiver = new ReturnMFGNumReceiver();
    private final BroadcastReceiver RetrunSetLampReceiver = new RetrunSetLampReceiver();
    private final BroadcastReceiver RetrunSetPGAReceiver = new RetrunSetPGAReceiver();
    private final BroadcastReceiver RetrunSetScanRepeatsReceiver = new RetrunSetScanRepeatsReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();

    private final IntentFilter scanDataReadyFilter = new IntentFilter(ISCNIRScanSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(ISCNIRScanSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(ISCNIRScanSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    private final IntentFilter scanStartedFilter = new IntentFilter(ISCNIRScanSDK.ACTION_SCAN_STARTED);
    private final IntentFilter SpectrumCalCoefficientsReadyFilter = new IntentFilter(ISCNIRScanSDK.SPEC_CONF_DATA);
    private final IntentFilter RetrunReadActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_READ_ACTIVATE_STATE);
    private final IntentFilter scanConfFilter = new IntentFilter(ISCNIRScanSDK.SCAN_CONF_DATA);
    private final IntentFilter RetrunActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE);
    private final IntentFilter  ReturnCurrentScanConfigurationDataFilter = new IntentFilter(ISCNIRScanSDK.RETURN_CURRENT_CONFIG_DATA);
    private final IntentFilter WriteScanConfigStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS);
    private final IntentFilter ReturnLampRampUpFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_RAMPUP_ADC);
    private final IntentFilter ReturnLampADCAverageFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_AVERAGE_ADC);
    private final IntentFilter ReturnMFGNumFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_MFGNUM);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ScanViewActivity.notifybackground";
    private String  NOTIFY_ISEXTVER = "com.Innospectra.NanoScan.ISEXTVER";
    //endregion
    //region parameter
    private ISCNIRScanSDK.ScanResults Scan_Spectrum_Data;
    private ISCNIRScanSDK mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;

    //Form HomeViewActivity->ScanViewActivity check chart view page parameter is initial or not
    Boolean init_viewpage_valuearray = false;
    //Record chart view page select
    int tabPosition = 0;

    //The name filter for BLE
    private static String DEVICE_NAME = "NIR";
    //Check the device is connected or not
    private boolean connected;
    //Get the device name you want to connect from the Settings page
    private String preferredDevice;
    //The active config of the device
    private ISCNIRScanSDK.ScanConfiguration activeConf;
    //Several configs in device were received
    private int receivedConfSize=-1;
    //Record the nnumber of the configs in the device
    private int storedConfSize;
    //Record the scan config list detail
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    //Record the scan config list detail from scan configuration page
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList_from_ScanConfiguration = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    //Record the active config byte
    private byte ActiveConfigByte[];
    //Record the scan config list byte
    private ArrayList <byte []> ScanConfig_Byte_List = new ArrayList<>();
    //Record the scan config list byte from scan configuration page
    private ArrayList <byte []> ScanConfig_Byte_List_from_ScanConfiuration = new ArrayList<>();
    //Record the active config index
    int ActiveConfigindex;

    private float minWavelength=900;
    private float maxWavelength=1700;
    private int MINWAV=900;
    private int MAXWAV=1700;
    private float minAbsorbance=0;
    private float maxAbsorbance=2;
    private float minReflectance=-2;
    private float maxReflectance=2;
    private float minIntensity=-7000;
    private float maxIntensity=7000;
    private float minReference=-7000;
    private float maxReference=7000;
    private int numSections=0;

    private Button btn_normal;
    private Button btn_quickset;
    private Button btn_manual;
    private Button btn_maintain;
    private Button btn_scan;
    private Button btn_report;
    //Normal scan setting
    private LinearLayout ly_normal_config;
    private EditText filePrefix;
    private TextView tv_normal_scan_conf;
    private ToggleButton toggle_btn_continuous_scan;
    private TextView tv_normal_interval_time;
    private EditText et_normal_interval_time;
    private TextView tv_normal_repeat;
    private EditText et_normal_scan_repeat;
    private Button btn_normal_continuous_stop;
    //Manual sacn setting
    private TextView tv_manual_scan_conf;
    private LinearLayout ly_manual_conf;
    private ToggleButton toggle_button_manual_scan_mode;
    private ToggleButton toggle_button_manual_lamp;
    private EditText et_manual_lamptime;
    private EditText et_manual_pga;
    private EditText et_manual_repead;
    private ScanMethod Current_Scan_Method = ScanMethod.Normal;
    //Quick set scan setting
    private EditText et_quickset_lamptime;
    private Spinner spin_quickset_scan_method;
    private Spinner spinner;
    private EditText et_quickset_spec_start;
    private EditText et_quickset_spec_end;
    private Spinner spin_quickset_scan_width;
    private TextView tv_quickset_res;
    private EditText et_quickset_res;
    private EditText et_quickset_average_scan;
    private Spinner spin_quickset_exposure_time;
    private ToggleButton toggle_btn_quickset_continuous_scan_mode;
    private TextView tv_quickset_scan_interval_time;
    private EditText et_quickset_scan_interval_time;
    private TextView tv_quickset_continuous_repeat;
    private EditText et_quickset_continuous_scan_repeat;
    private Button btn_quickset_continuous_scan_stop;
    private Button btn_quickset_set_config;

    int quickset_scan_method_index =0;
    int quickset_exposure_time_index =0;
    int quickset_scan_width_index = 2;
    private int continuous_count=0;
    Boolean show_finish_continous_dialog = false;
    public static boolean showActiveconfigpage = false;
    private int quickset_init_start_nm;
    private int quickset_init_end_nm;
    private int quickset_init_res;
    //Maintain (reference) scan setting
    private ToggleButton Toggle_Button_maintain_reference;

    //When read the activate status of the device, check this from HomeViewActivity->ScanViewActivity trigger or not
    private  String mainflag = "";
    //Check spectrum calibration coefficient is received or not
    Boolean downloadspecFlag = false;
    //Record spectrum calibration coefficient
    byte[] SpectrumCalCoefficients = new byte[144];
    //Allow AddScanConfigViewActivity to get the spectrum calibration coefficient to calculate max pattern
    public static byte []passSpectrumCalCoefficients = new byte[144];

    boolean stop_continuous = false;
    int MaxPattern = 0;
    Boolean isScan = false;
    //Record is set config for reference scan or not
    boolean reference_set_config = false;

    //Is go to Scan Configuration page or not
    public static boolean GotoScanConfigFlag = false;
    //On pause event trigger is go to other page or not
    private static Boolean GotoOtherPage = false;
    public  static Boolean isOldTiva = false;
    //endregion
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter, "NIR");
        Bundle bundle = getIntent().getExtras();
        mainflag = bundle.getString("main");
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton, true);
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);

        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        calProgress.setVisibility(View.VISIBLE);
        progressBarinsideText = (TextView) findViewById(R.id.progressBarinsideText);
        connected = false;
        Disable_Stop_Continous_button();

        filePrefix = (EditText) findViewById(R.id.et_prefix);
        btn_scan = (Button) findViewById(R.id.btn_scan);

        setScanBtnStatus(false, getResources().getString(R.string.text_scanpredict));
        btn_scan.setOnClickListener(Button_Scan_Click);

        btn_report = (Button) findViewById(R.id.btn_report);
        setReportBtnStatus(false);
        btn_report.setOnClickListener(Button_Scan_Report_Click);


        setActivityTouchDisable(true);

        //20210208 zhaozz：尝试屏蔽其他标签
        InitialNormalComponent();
        InitialQuicksetComponent();
        InitialManualComponent();
        InitialMaintainComponent();
        InitialScanMethodButtonComponent();
        TitleBarEvent();

        //Bind to the service. This will start it, and call the start command function
        Intent gattServiceIntent = new Intent(this, ISCNIRScanSDK.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //region Register all needed broadcast receivers
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanDataReadyReceiver, scanDataReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefDataReadyReceiver, refReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(NotifyCompleteReceiver, notifyCompleteFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefCoeffDataProgressReceiver, requestCalCoeffFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(CalMatrixDataProgressReceiver, requestCalMatrixFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfReceiver, scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanStartedReceiver, scanStartedFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(SpectrumCalCoefficientsReadyReceiver, SpectrumCalCoefficientsReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunReadActivateStatusReceiver, RetrunReadActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunActivateStatusReceiver, RetrunActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnCurrentScanConfigurationDataReceiver, ReturnCurrentScanConfigurationDataFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetUUIDReceiver, new IntentFilter(ISCNIRScanSDK.SEND_DEVICE_UUID));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampRampUpADCReceiver, ReturnLampRampUpFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampADCAverageReceiver, ReturnLampADCAverageFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnMFGNumReceiver, ReturnMFGNumFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunSetLampReceiver, new IntentFilter(ISCNIRScanSDK.SET_LAMPSTATE_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunSetPGAReceiver, new IntentFilter(ISCNIRScanSDK.SET_PGA_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunSetScanRepeatsReceiver, new IntentFilter(ISCNIRScanSDK.SET_SCANREPEATS_COMPLETE));
        //endregion


        // 在我们的这个位置的话创建我们的数组
        Intent intent = getIntent();
        String token = intent.getStringExtra("token");
        String url = "http://211.82.95.146:5005/category";
        final List<String> item = new ArrayList<String>();
        OkHttpClientUtil.get(url, token).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("TAG", "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                if (json.indexOf("category") != -1) {
                    JSONArray jsonArray = JSONArray.parseArray(json);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        item.add(jsonArray.getJSONObject(i).get("category").toString());
                    }
                    System.out.println(item);
                }
                @SuppressLint("ResourceType")
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, item);
                // 设置我们的数组下拉时的选项的样式
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // 将我们的适配器和我们的下拉列表框关联起来
                Spinner spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setAdapter(adapter);
            }
        });

        //20210208 zhaozz: 尝试启动时隐藏部分控件
        setAdminControlsVisiable(true);
    }

    // 20210208 zhaozz： 尝试添加方法控制控件的显示和隐藏
    private  void setAdminControlsVisiable(boolean flag) {
        if (flag) {
            findViewById(R.id.layout_maintain).setVisibility(View.VISIBLE);
            // 尝试显示按钮
            findViewById(R.id.reference).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_reference).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);

            // 尝试隐藏界面下方的菜单栏
            findViewById(R.id.btn_normal).setVisibility(View.GONE);
            findViewById(R.id.btn_quickset).setVisibility(View.GONE);
            findViewById(R.id.btn_manual).setVisibility(View.GONE);
            findViewById(R.id.btn_maintain).setVisibility(View.GONE);

            btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.Normal;

//            ActionBar ab = getActionBar();
//            if (ab != null) {
//                mViewPager.setCurrentItem(2);
//            }
        }
        else {
            //TODO:恢复管理员状态，正常应该不用操作
        }
    }

    private Button.OnClickListener Continuous_Scan_Stop_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            stop_continuous = true;
        }
    };
    //region Scan device and connect
    //Manage service lifecycle
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Get a reference to the service from the service connection
            mNanoBLEService = ((ISCNIRScanSDK.LocalBinder) service).getService();

            //initialize bluetooth, if BLE is not available, then finish
            if (!mNanoBLEService.initialize()) {
                finish();
            }
            //Start scanning for devices that match DEVICE_NAME
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if(mBluetoothLeScanner == null){
                finish();
                Toast.makeText(ScanViewActivity.this, "请确认蓝牙已打开并重试", Toast.LENGTH_SHORT).show();//Please ensure Bluetooth is enabled and try again
            }
            mHandler = new Handler();
            if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null) != null) {
                preferredDevice = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                scanPreferredLeDevice(true);
            } else {
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };

    /**
     * Callback function for Bluetooth scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivity#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link  ISCNIRScanSDK#SCAN_PERIOD} has not expired
     */
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            String preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);

            if (name != null) {
                if (device.getName().contains(DEVICE_NAME) && device.getAddress().equals(preferredNano)) {
                    mNanoBLEService.connect(device.getAddress());
                    connected = true;
                    scanLeDevice(false);
                }
            }
        }
    };

    /**
     * Callback function for preferred Nano scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivity#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link ISCNIRScanSDK#SCAN_PERIOD} has not expired
     */
    private final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            String preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
            if (name != null) {
                if (device.getName().contains(DEVICE_NAME) && device.getAddress().equals(preferredNano)) {
                    if (device.getAddress().equals(preferredDevice)) {
                        mNanoBLEService.connect(device.getAddress());
                        connected = true;
                        scanPreferredLeDevice(false);
                    }
                }
            }
        }
    };

    /**
     * Scans for Bluetooth devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivity#mLeScanCallback}
     * @param enable Tells the Bluetooth adapter {@link ISCNIRScanSDK#mBluetoothAdapter} if
     *  it should start or stop scanning
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            notConnectedDialog();
                        }
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                finish();
                Toast.makeText(ScanViewActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * Scans for preferred Nano devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivity#mPreferredLeScanCallback}
     *
     * @param enable Tells the Bluetooth adapter {@link ISCNIRScanSDK#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    private void scanPreferredLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
                    if (!connected) {

                        scanLeDevice(true);
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if(mBluetoothLeScanner == null)
            {
                notConnectedDialog();
            }
            else
            {
                mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
            }

        } else {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }
    //endregion
    //region After connect to device
    /**
     * Custom receiver that will request the time once all of the GATT notifications have been subscribed to
     * If the connected device has saved the last setting, skip request the time. Read the device's  active config directly.
     */
    public class NotifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Boolean reference = false;
            if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
            {
                reference = true;
            }
            if(preferredDevice.equals(HomeViewActivity.storeCalibration.device) && reference == false)
            {
                byte[] refCoeff = HomeViewActivity.storeCalibration.storrefCoeff;
                byte[] refMatrix = HomeViewActivity.storeCalibration.storerefMatrix;
                ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
                refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
                ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                //Get active config
                ISCNIRScanSDK.GetActiveConfig();
            }
            else
            {
                if(reference == true)
                {
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not");
                }
                //After ser current time will trigger to download calibration coefficient and  calibration matrix
                ISCNIRScanSDK.SetCurrentTime();
            }
        }
    }
    /**
     * Custom receiver for receiving calibration coefficient data.(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class RefCoeffDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }
    /**
     * Custom receiver for receiving calibration matrix data. When this receiver action complete, it
     * will request the active configuration so that it can be displayed in the listview(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class CalMatrixDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
            if (size) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
                //Send broadcast GET_ACTIVE_CONF will trigger to get active config
                ISCNIRScanSDK.GetActiveConfig();
            }
        }
    }
    /**
     * After download reference calibration  matrix will notify and save(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class RefDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            byte[] refCoeff = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_COEF_DATA);
            byte[] refMatrix = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
            ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
            calProgress.setVisibility(View.GONE);
            //------------------------------------------------------------------
            HomeViewActivity.storeCalibration.device = preferredDevice;
            HomeViewActivity.storeCalibration.storrefCoeff = refCoeff;
            HomeViewActivity.storeCalibration.storerefMatrix = refMatrix;
        }
    }
    /**
     * Send broadcast  GET_ACTIVE_CONF will  through GetActiveScanConfReceiver to get active config(ISCNIRScanSDK.GetActiveConfig() should be called)
     */
    private class  GetActiveScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ActiveConfigindex = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)[0];
            if(ScanConfigList.size()!=0)
            {
                GetActiveConfigOnResume();
            }
            else
            {
                //Get the number of scan config and scan config data
                ISCNIRScanSDK.GetScanConfig();
            }
        }
    }
    /**
     * Get the number of scan config(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class  ScanConfSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            storedConfSize = intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0);
        }
    }
    /**
     *Get the scan config data(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receivedConfSize++;
            ScanConfig_Byte_List.add(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            ScanConfigList.add(ISCNIRScanSDK.scanConf);

            if (storedConfSize>0 && receivedConfSize==0) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.reading_configurations));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(storedConfSize);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(receivedConfSize+1);
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax() || barProgressDialog.getMax()==1)
            {
                for(int i=0;i<ScanConfigList.size();i++)
                {
                    int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                    if(ActiveConfigindex == ScanConfigIndextoByte )
                    {
                        activeConf = ScanConfigList.get(i);
                        ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    }
                }
                barProgressDialog.dismiss();
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, ISCNIRScanSDK.scanConf.getConfigName());
                if(null != activeConf){
                    tv_normal_scan_conf.setText(activeConf.getConfigName());
                    tv_manual_scan_conf.setText(activeConf.getConfigName());
                }

                if(downloadspecFlag ==false)
                {
                    //Get spectrum calibration coefficient
                    ISCNIRScanSDK.GetSpectrumCoef();
                    downloadspecFlag = true;
                }
            }
        }
    }
    /**
     * Get  spectrum calibration coefficient from the device then send request to get the device info(ISCNIRScanSDK.GetSpectrumCoef() should be called)
     */
    public class SpectrumCalCoefficientsReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            SpectrumCalCoefficients = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_SPEC_COEF_DATA);
            passSpectrumCalCoefficients = SpectrumCalCoefficients;
            //Request device information
            ISCNIRScanSDK.GetDeviceInfo();
        }
    }

    String model_name="";
    String model_num = "";
    String serial_num = "";
    String HWrev = "";
    String Tivarev ="";
    String Specrev = "";
    /**
     * Send broadcast  GET_INFO will  through DeviceInfoReceiver  to get the device info(ISCNIRScanSDK.GetDeviceInfo() should be called)
     */
    public class DeviceInfoReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            model_name = intent.getStringExtra(ISCNIRScanSDK.EXTRA_MODEL_NUM);
            serial_num = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SERIAL_NUM);
            HWrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_HW_REV);
            Tivarev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_TIVA_REV);
            Specrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SPECTRUM_REV);
            if(Tivarev.substring(0,1) .equals("3") && (HWrev.substring(0,1).equals("E")|| HWrev.substring(0,1).equals("O")))
                isExtendVer = true;
            else
                isExtendVer = false;
            if(HWrev.substring(0,1).equals("N"))
                Dialog_Pane_Finish("Not support","Not to support the N version of the main board.\nWill go to the home page.");
            else
            {
                //Send broadcast to notify NanoBLEService to know the device is extension or not
                Notify_IsEXTVersion();
                GetFWLevel(Tivarev);
                InitParameter();
                if(fw_level.compareTo(FW_LEVEL.LEVEL_0)>0)
                {
                    //Request device MFG num
                    ISCNIRScanSDK.GetMFGNumber();
                }
                else
                {
                    Dialog_Pane_Finish("Firmware Out of Date","You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                            "FW required version at least V2.4.4.\nDetected version is V" + Tivarev +".");
                }
                //Request device MFG num
                ISCNIRScanSDK.GetMFGNumber();
            }
        }
    }
    /**
     *Get MFG Num (ISCNIRScanSDK.GetMFGNumber() should be called)
     */
    private byte MFG_NUM[];
    public class ReturnMFGNumReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            MFG_NUM = intent.getByteArrayExtra(ISCNIRScanSDK.MFGNUM_DATA);
            //Get the uuid of the device
            ISCNIRScanSDK.GetUUID();
        }
    }
    /**
     *  Define FW LEVEL according to tiva version
     * @param Tivarev  Tiva version of the device
     */
    private void GetFWLevel(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level = FW_LEVEL.LEVEL_0;
        if(isExtendVer)
        {
           if(Integer.parseInt(TivaArray[1])>=3 && split_hw[0].equals("O"))
           {
                 /*New Applications:
                  1. Support read ADC value
                 */
                fw_level = FW_LEVEL.LEVEL_EXT_2;//>=3.3.0 and main board = "O"
           }
            else if(Integer.parseInt(TivaArray[1])>=3)
            {
                /*New Applications:
                  1. Add Lock Button
                 */
                fw_level = FW_LEVEL.LEVEL_EXT_1;//>=3.3.0
            }
            else if(Integer.parseInt(TivaArray[1])==2 && Integer.parseInt(TivaArray[2])==1 )
                fw_level = FW_LEVEL.LEVEL_EXT_1;//==3.2.1
        }
        else
        {
            if(Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3 &&split_hw[0].equals("F"))
            {
                /*New Applications:
                  1. Support read ADC value
                 */
                fw_level = FW_LEVEL.LEVEL_3;//>=2.4.4 and main board ="F"
            }
            else if(Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3)
            {
                /*New Applications:
                  1. Add Lock Button
                 */
                fw_level = FW_LEVEL.LEVEL_2;//>=2.4.4
            }
            else if((TivaArray.length==3 && Integer.parseInt(TivaArray[1])>=1)|| (TivaArray.length==4 &&  Integer.parseInt(TivaArray[3])>=67))//>=2.1.0.67
            {
                 //New Applications:
                 // 1. Support activate state

                fw_level = FW_LEVEL.LEVEL_1;
            }
            else
            {
                fw_level = FW_LEVEL.LEVEL_0;
            }
        }
    }
    /**
     *  Determine the wavelength range of the device and parameter initialization
     */
    private void InitParameter()
    {
        if(isExtendVer)
        {
            minWavelength = 1350;
            maxWavelength = 2150;
            MINWAV = 1350;
            MAXWAV = 2150;
        }
        else
        {
            minWavelength = 900;
            maxWavelength = 1700;
            MINWAV = 900;
            MAXWAV = 1700;
        }
        et_quickset_spec_start.setText(Integer.toString(MINWAV));
        et_quickset_spec_end.setText(Integer.toString(MAXWAV));
        quickset_init_start_nm = (Integer.parseInt(et_quickset_spec_start.getText().toString()));
        quickset_init_end_nm = (Integer.parseInt(et_quickset_spec_end.getText().toString()));
        //not support lock button
        if(fw_level.compareTo(FW_LEVEL.LEVEL_1) <=0)
        {
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
        }
    }
    /**
     * Get the device uuid(ISCNIRScanSDK.GetUUID()should be called)
     */
    String uuid="";
    public class GetUUIDReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte buf[] = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEVICE_UUID);
            for(int i=0;i<buf.length;i++)
            {
                uuid += Integer.toHexString( 0xff & buf[i] );
                if(i!= buf.length-1)
                {
                    uuid +=":";
                }
            }
            CheckIsOldTIVA();
            if(!isOldTiva)
            {
                //Get the device is activate or not
                ISCNIRScanSDK.ReadActivateState();
            }
            else
            {
                closeFunction();
                mMenu.findItem(R.id.action_key).setVisible(false);
            }
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        }
    }

    /**
     * Get the activate state of the device(ISCNIRScanSDK.ReadActivateState() should be called)
     */
    public class RetrunReadActivateStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if(mainflag!="")//Only from HomeViewActivity->ScanViewActivity should do this
            {
                //set active scan config avoid the device use wpf or winform local config to set config in device
                ISCNIRScanSDK.SetActiveConfig();
                mainflag = "";
            }
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_READ_ACTIVATE_STATE);
            if(state[0] == 1)
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){

                    @Override
                    public void run() {
                        SetDeviceButtonStatus();
                        Dialog_Pane_OpenFunction("设备已激活","设备内置算法均已解锁");//device activated+Device advanced functions are all unlocked."
                    }}, 200);
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                mMenu.findItem(R.id.action_key).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                String licensekey = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, null);
                //The device is locked but saved license key
                if(licensekey!=null && licensekey!="")
                {
                    calProgress.setVisibility(View.VISIBLE);
                    String filterdata = filterDate(licensekey);
                    final byte data[] = hexToBytes(filterdata);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            ISCNIRScanSDK.SetLicenseKey(data);
                        }}, 200);
                }
                else
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            SetDeviceButtonStatus();
                            Dialog_Pane("设备未激活","设备内置算法未解锁.");//Unlock device+Some functions are locked.
                        }}, 200);
                    mMenu.findItem(R.id.action_settings).setEnabled(true);
                    mMenu.findItem(R.id.action_key).setEnabled(true);
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                    closeFunction();
                }
            }
        }
    }
    /**
     *  Get the activate state of the device(ISCNIRScanSDK.SetLicenseKey(data) should be called)
     */
    public class RetrunActivateStatusReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            mMenu.findItem(R.id.action_settings).setEnabled(true);
            mMenu.findItem(R.id.action_key).setEnabled(true);
            calProgress.setVisibility(View.GONE);
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_ACTIVATE_STATUS);
            if(state[0] == 1)
            {
                SetDeviceButtonStatus();
                Dialog_Pane_OpenFunction("Device Activated","Device advanced functions are all unlocked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                SetDeviceButtonStatus();
                Dialog_Pane("Unlock device","Some functions are locked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                closeFunction();
            }
        }
    }
    private void CheckIsOldTIVA()
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        try {
            if(!isExtendVer && (Integer.parseInt(TivaArray[1])<4 || Integer.parseInt(TivaArray[1])<4))//Tiva <2.4.4(the newest version)
            {
                isOldTiva = true;
                Dialog_Pane_OldTIVA("Firmware Out of Date", "You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                        "FW required version at least V2.4.4\nDetected version is V" + Tivarev + "\nDo you still want to continue?");
            }
            else
                isOldTiva = false;
        }catch (Exception e)
        {

        };
    }
    //endregion

    //20210224 zhaozz: 尝试增加标记位区分管理员和普通用户的视图区别
    private  boolean isAdminRole = false;

    //region title bar
    /**
     * Initial chart view pager and title bar event
     */
    private void TitleBarEvent()
    {
        //Set up title bar and  enable tab navigation
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.new_scan));
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mViewPager = (NoScrollViewPager) findViewById(R.id.viewpager);
            mViewPager.setOffscreenPageLimit(2);

            // Create a tab listener that is called when the user changes tabs.
            ActionBar.TabListener tl = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    //1.if select tab0 then scan, onTabSelected can't invoke. But select other tab can invoke.
                    if(isScan)
                    {
                        if(tabPosition == 0) //2. select tab0 then scan. Choose tab1.at this time isscan =true but tabPosition will be equal to 0 will cause page error.
                        //So if tabPosition is 0, it will choose to do mViewPager.setCurrentItem (tab.getPosition ()); to see the current state
                        {
                            mViewPager.setCurrentItem(tab.getPosition());
                        }
                        else//The tabPosition will record the current tab and then update after the scan
                        {
                            mViewPager.setCurrentItem(tabPosition);
                        }
                        isScan = false;
                    }
                    else
                    {
                        mViewPager.setCurrentItem(tab.getPosition());
                    }
                }
                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }
                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }
            };
            // 当为管理员登陆时，增加4个标签和绑定监听，否则仅增加一个标签
            if (isAdminRole) {
                // Add 3 tabs, specifying the tab's text and TabListener
                for (int i = 0; i < 4; i++) {
                    ab.addTab(
                            ab.newTab()
                                    .setText(getResources().getStringArray(R.array.graph_tab_index)[i])
                                    .setTabListener(tl));
                }
            }
            else{
                ab.addTab(ab.newTab().setText(getResources().getStringArray(R.array.graph_tab_index)[1]).setTabListener(tl));
            }
        }
    }
    /**
    * Inflate the options menu so that user actions are present
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        mMenu = menu;
        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
        return true;
    }

    /**
     * Handle the selection of a menu item.
     * In this case, the user has the ability to access settings while the Nano is connected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            GotoOtherPage = true;
            ChangeLampState();
            //avoid conflict when go to scan config page
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);

            Intent configureIntent = new Intent(mContext, ConfigureViewActivity.class);
            startActivity(configureIntent);
        }
        if (id == R.id.action_key) {
            GotoOtherPage = true;
            ChangeLampState();
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
            Intent configureIntent = new Intent(mContext, ActivationViewActivity.class);
            startActivity(configureIntent);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        }
        if (id == android.R.id.home) {
            ChangeLampState();
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion
    //region Initial Component and control
    /**
     * Initial the button event of the scan mode
     */
    private void InitialScanMethodButtonComponent()
    {
        btn_normal = (Button) findViewById(R.id.btn_normal);
        btn_quickset = (Button) findViewById(R.id.btn_quickset);
        btn_manual = (Button) findViewById(R.id.btn_manual);
        btn_maintain = (Button) findViewById(R.id.btn_maintain);

        btn_normal.setOnClickListener(Button_Normal_Click);
        btn_normal.setClickable(false);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_quickset.setOnClickListener(Button_Quicket_Click);
        btn_quickset.setClickable(false);
        btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_manual.setOnClickListener(Button_Manual_Click);
        btn_manual.setClickable(false);
        btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_maintain.setOnClickListener(Button_Maintain_Click);
        btn_maintain.setClickable(false);
        btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
    }
    /**
     * Initial the component of the normal scan mode
     */
    private void InitialNormalComponent()
    {
        tv_normal_scan_conf = (TextView) findViewById(R.id.tv_scan_conf);
        ly_normal_config = (LinearLayout) findViewById(R.id.ll_conf);
        tv_normal_repeat = (TextView) findViewById(R.id.tv_normal_repeat);
        toggle_btn_continuous_scan = (ToggleButton) findViewById(R.id.btn_continuous);
        tv_normal_interval_time = (TextView) findViewById(R.id.tv_normal_interval_time);
        et_normal_interval_time = (EditText) findViewById(R.id.et_normal_interval_time);
        et_normal_scan_repeat = (EditText) findViewById(R.id.et_normal_repeat);
        btn_normal_continuous_stop = (Button)findViewById(R.id.btn_continuous_stop);

        ly_normal_config.setClickable(false);
        ly_normal_config.setOnClickListener(Normal_Config_Click);
        toggle_btn_continuous_scan.setChecked(getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.continuousScan, false));
        toggle_btn_continuous_scan.setOnClickListener(Normal_Continuous_Scan_Click);
        btn_normal_continuous_stop.setOnClickListener(Continuous_Scan_Stop_Click);
        et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_scan_repeat.setOnEditorActionListener(Normal_Continuous_Scan_Repeat_OnEdit);
        tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
    }
    /**
     * Initial the component of the quick set  scan mode
     */
    private void InitialQuicksetComponent()
    {
        et_quickset_lamptime = (EditText)findViewById(R.id.et_prefix_lamp_quickset);
        spin_quickset_scan_method = (Spinner)findViewById(R.id.spin_scan_method);
        et_quickset_spec_start = (EditText)findViewById(R.id.et_spec_start);
        et_quickset_spec_end = (EditText)findViewById(R.id.et_spec_end);
        spin_quickset_scan_width = (Spinner)findViewById(R.id.spin_scan_width);
        et_quickset_res = (EditText)findViewById(R.id.et_res);
        et_quickset_average_scan = (EditText)findViewById(R.id.et_aver_scan);
        spin_quickset_exposure_time = (Spinner)findViewById(R.id.spin_time);
        toggle_btn_quickset_continuous_scan_mode = (ToggleButton)findViewById(R.id.btn_continuous_scan_mode);
        tv_quickset_scan_interval_time = (TextView)findViewById(R.id.tv_quickset_scan_interval_time);
        et_quickset_scan_interval_time = (EditText)findViewById(R.id.scan_interval_time);
        tv_quickset_continuous_repeat = (TextView)findViewById(R.id.tv_quickset_continuous_repeat);
        et_quickset_continuous_scan_repeat = (EditText)findViewById(R.id.et_repeat_quick);
        btn_quickset_continuous_scan_stop = (Button)findViewById(R.id.btn_continuous_stop_quick);
        btn_quickset_set_config = (Button)findViewById(R.id.btn_set_value);
        tv_quickset_res = (TextView)findViewById(R.id.tv_res);

        et_quickset_lamptime.setOnEditorActionListener(Quickset_Lamp_Time_OnEditor);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.scan_method_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_scan_method.setAdapter(adapter);
        spin_quickset_scan_method.setOnItemSelectedListener(Quickset_Scan_Method_ItemSelect);
        et_quickset_spec_start.setOnEditorActionListener(Quickset_Spec_Start_OnEditor);
        et_quickset_spec_end.setOnEditorActionListener(Quickset_Spec_End_OnEditor);
        ArrayAdapter<CharSequence> adapter_width = ArrayAdapter.createFromResource(this,
                R.array.scan_width, android.R.layout.simple_spinner_item);
        adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_scan_width.setAdapter(adapter_width);
        spin_quickset_scan_width.setOnItemSelectedListener(Quickset_Scan_Width_ItemSelect);
        et_quickset_res.setOnEditorActionListener(Quickset_Res_OnEditor);
        ArrayAdapter<CharSequence> adapter_time = ArrayAdapter.createFromResource(this,
                R.array.exposure_time, android.R.layout.simple_spinner_item);
        adapter_time.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_exposure_time.setAdapter(adapter_time);
        spin_quickset_exposure_time.setOnItemSelectedListener(Quickset_Exposure_Time_ItemSelect);
        toggle_btn_quickset_continuous_scan_mode.setOnClickListener(QuickSet_Continuous_Scan_Click);
        et_quickset_continuous_scan_repeat.setOnEditorActionListener(QuickSet_Continuous_Scan_Repeat_OnEdit);
        btn_quickset_continuous_scan_stop.setOnClickListener(Continuous_Scan_Stop_Click);
        btn_quickset_set_config.setOnClickListener(Quickset_Set_Config_Click);

        quickset_init_start_nm = (Integer.parseInt(et_quickset_spec_start.getText().toString()));
        quickset_init_end_nm = (Integer.parseInt(et_quickset_spec_end.getText().toString()));
        quickset_init_res = (Integer.parseInt(et_quickset_res.getText().toString()));
        tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
    }
    /**
     * Initial the component of the manual scan mode
     */
    private void InitialManualComponent()
    {
        tv_manual_scan_conf = (TextView)findViewById(R.id.tv_scan_conf_manual) ;
        toggle_button_manual_scan_mode = (ToggleButton) findViewById(R.id.btn_scan_mode);
        toggle_button_manual_lamp = (ToggleButton) findViewById(R.id.btn_lamp);
        et_manual_lamptime = (EditText) findViewById(R.id.et_prefix_lamp);
        et_manual_pga = (EditText) findViewById(R.id.et_pga);
        et_manual_repead = (EditText) findViewById(R.id.et_repeat);
        ly_manual_conf = (LinearLayout)findViewById(R.id.ly_conf_manual);

        toggle_button_manual_scan_mode.setOnClickListener(Toggle_Button_Manual_ScanMode_Click);
        toggle_button_manual_lamp.setOnCheckedChangeListener(Toggle_Button_Manual_Lamp_Changed);
        et_manual_pga.setOnEditorActionListener(Manual_PGA_OnEditor);
        et_manual_repead.setOnEditorActionListener(Manual_Repeat_OnEditor);
        et_manual_lamptime.setOnEditorActionListener(Manual_Lamptime_OnEditor);
        ly_manual_conf.setOnClickListener(Manual_Config_Click);
        toggle_button_manual_lamp.setEnabled(false);
        et_manual_repead.setEnabled(false);
        et_manual_pga.setEnabled(false);
        et_manual_lamptime.setEnabled(false);
    }
    /**
     * Initial the component of the matain(reference) scan mode
     */
    private void InitialMaintainComponent()
    {
        Toggle_Button_maintain_reference = (ToggleButton)findViewById(R.id.btn_reference);
    }
    private void DisableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(false);
        }
    }
    private void DisableAllComponent()
    {
        //normal------------------------------------------------
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_prefix);
        DisableLinearComponet(layout);
        // layout = (LinearLayout) findViewById(R.id.ll_os);
        // DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_interval_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_repeat);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        DisableLinearComponet(layout);
        //manual-----------------------------------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_conf);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        DisableLinearComponet(layout);
        //quick set ----------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_prefix_quickset);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_method);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_start);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_end);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_width);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_res);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_aver_scan);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_ex_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_continus_scan_mode);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_interval_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat_quick);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_set_value);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        DisableLinearComponet(layout);
        //maintain------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ly_reference);
        DisableLinearComponet(layout);
        //------------------------------------------
        btn_scan.setClickable(false);
        btn_normal.setClickable(false);
        btn_quickset.setClickable(false);
        btn_manual.setClickable(false);
        btn_maintain.setClickable(false);
        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
    }

    private void Disable_Stop_Continous_button()
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        DisableLinearComponet(layout);
    }

    private void Enable_Stop_Continous_button()
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        EnableLinearComponet(layout);
    }

    private void EnableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(true);
        }
    }
    private void EnableAllComponent()
    {
        //normal------------------------------------------
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_prefix);
        EnableLinearComponet(layout);
        // layout = (LinearLayout) findViewById(R.id.ll_os);
        // EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_interval_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_repeat);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        EnableLinearComponet(layout);
        tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        //manual-------------------------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_conf);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        EnableLinearComponet(layout);
        if(toggle_button_manual_scan_mode.isChecked() == false)
        {
            toggle_button_manual_lamp.setEnabled(false);
            et_manual_repead.setEnabled(false);
            et_manual_pga.setEnabled(false);
            et_manual_lamptime.setEnabled(true);
        }
        //quick set ----------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_prefix_quickset);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_method);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_start);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_end);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_width);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_res);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_aver_scan);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_ex_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_continus_scan_mode);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_interval_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat_quick);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_set_value);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        EnableLinearComponet(layout);
        tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        //maintain------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ly_reference);
        EnableLinearComponet(layout);
        //------------------------------------------
        btn_scan.setClickable(true);
        btn_normal.setClickable(true);
        btn_quickset.setClickable(true);
        btn_manual.setClickable(true);
        btn_maintain.setClickable(true);
        setActivityTouchDisable(false);
        mMenu.findItem(R.id.action_settings).setEnabled(true);
        mMenu.findItem(R.id.action_key).setEnabled(true);
    }
    /**
     * Unlock device will open all scan mode
     */
    private void openFunction()
    {
        btn_normal.setClickable(true);
        btn_quickset.setClickable(true);
        btn_manual.setClickable(true);
        btn_maintain.setClickable(true);
        btn_manual.setBackgroundColor(0xFF0099CC);
        btn_quickset.setBackgroundColor(0xFF0099CC);
        btn_maintain.setBackgroundColor(0xFF0099CC);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));

        findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);

        Current_Scan_Method = ScanMethod.Normal;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);
    }
    /**
     * %%锁定设备只能使用普通扫描
     * Lock device can only use normal scan
     */
    private void closeFunction()
    {
        btn_quickset.setClickable(false);
        btn_manual.setClickable(false);
        btn_maintain.setClickable(false);
        btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        Current_Scan_Method = ScanMethod.Normal;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);
    }
    //endregion
    //region Scan Method Button Event
    private Button.OnClickListener Button_Normal_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.Normal;
            //----------------------------------------------------
            if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.").contains("Activated"))
            {
                openFunction();
            }
            else
            {
                closeFunction();
            }
        }
    };
    private Button.OnClickListener Button_Quicket_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_quickset).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.QuickSet;
            //---------------------------------------------------------------
            UI_ShowMaxPattern();

        }
    };
    private Button.OnClickListener Button_Manual_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.layout_manual).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            if(Current_Scan_Method != ScanMethod.Manual)//State Normal,Quickset,Maintan -> Manual
            {
                toggle_button_manual_scan_mode.setChecked(false);
                toggle_button_manual_lamp.setEnabled(false);
                et_manual_repead.setEnabled(false);
                et_manual_pga.setEnabled(false);
                et_manual_lamptime.setEnabled(true);
                et_manual_repead.setText("6");
                et_manual_pga.setText("1");
                et_manual_lamptime.setText("625");
            }
            Current_Scan_Method = ScanMethod.Manual;
        }
    };
    private Button.OnClickListener Button_Maintain_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_maintain).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.Maintain;
        }
    };
    private void ChangeLampState()
    {
        if(Current_Scan_Method == ScanMethod.Manual && toggle_button_manual_scan_mode.isChecked())//Manual->Normal,Quickset,Maintain
        {
            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setChecked(false);//close lamp
            }
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
        }
    }
    //endregion
    //region Normal UI component Event
    private LinearLayout.OnClickListener Normal_Config_Click = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    private Button.OnClickListener Normal_Continuous_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
            tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
            tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
            et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        }
    };
    private EditText.OnEditorActionListener Normal_Continuous_Scan_Repeat_OnEdit = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                int value = Integer.parseInt(et_normal_scan_repeat.getText().toString());
                if(value<=1)
                {
                    NotValidValueDialog("Warning","The number of continuous scan repeats should be larger than 1.");
                    et_normal_scan_repeat.setText("2");
                }
            }
            return false;
        }
    };
    //endregion
    //region QuickSet UI component Event
    private EditText.OnEditorActionListener Quickset_Lamp_Time_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(Integer.parseInt(et_quickset_lamptime.getText().toString())!=625)
                {
                    int lamptime = Integer.parseInt(et_quickset_lamptime.getText().toString());
                    ISCNIRScanSDK.SetLampStableTime(lamptime);
                }
                return false; // consume.
            }
            return false;
        }
    };

    private Spinner.OnItemSelectedListener Quickset_Scan_Method_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_scan_method_index = i;
            UI_ShowMaxPattern();
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    private EditText.OnEditorActionListener Quickset_Spec_Start_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_spec_start.getText().toString().matches("")|| Integer.parseInt(et_quickset_spec_start.getText().toString())> Integer.parseInt(et_quickset_spec_end.getText().toString()) || Integer.parseInt(et_quickset_spec_start.getText().toString())<MINWAV)
                {
                    et_quickset_spec_start.setText(Integer.toString(quickset_init_start_nm));
                    Dialog_Pane("Error","Start wavelength should be between " + MINWAV + "nm and end wavelength!");
                    return false; // consume.
                }
            }
            quickset_init_start_nm = Integer.parseInt(et_quickset_spec_start.getText().toString());
            UI_ShowMaxPattern();
            return false;
        }
    };
    private EditText.OnEditorActionListener Quickset_Spec_End_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_spec_end.getText().toString().matches("")|| Integer.parseInt(et_quickset_spec_end.getText().toString())< Integer.parseInt(et_quickset_spec_start.getText().toString()) || Integer.parseInt(et_quickset_spec_end.getText().toString())>MAXWAV)
                {
                    et_quickset_spec_end.setText(Integer.toString(quickset_init_end_nm));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + MAXWAV + "nm!");
                    return false; // consume.
                }
            }
            quickset_init_end_nm = Integer.parseInt(et_quickset_spec_end.getText().toString());
            UI_ShowMaxPattern();
            return false;
        }
    };
    private Spinner.OnItemSelectedListener Quickset_Scan_Width_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_scan_width_index = i+2;
            UI_ShowMaxPattern();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
    private EditText.OnEditorActionListener Quickset_Res_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_res.getText().toString().matches("")|| Integer.parseInt(et_quickset_res.getText().toString())< 2 || Integer.parseInt(et_quickset_res.getText().toString())>MaxPattern)
                {
                    et_quickset_res.setText(Integer.toString(quickset_init_res));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + ".");
                    return false; // consume.

                }
            }
            quickset_init_res = Integer.parseInt(et_quickset_res.getText().toString());
            return false;
        }
    };
    private Spinner.OnItemSelectedListener Quickset_Exposure_Time_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_exposure_time_index = i;
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };
    private Button.OnClickListener Quickset_Set_Config_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(checkQuicksetValue())
            {
                btn_quickset_set_config.setClickable(false);
                btn_scan.setClickable(false);
                calProgress.setVisibility(view.VISIBLE);
                byte[] EXTRA_DATA = ChangeScanConfigToByte();
                ISCNIRScanSDK.ScanConfig(EXTRA_DATA,ISCNIRScanSDK.ScanConfig.SET);
            }
        }
    };
    private EditText.OnEditorActionListener QuickSet_Continuous_Scan_Repeat_OnEdit = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                int value = Integer.parseInt(et_quickset_continuous_scan_repeat.getText().toString());
                if(value<=1)
                {
                    NotValidValueDialog("Warning","The number of continuous scan repeats should be larger than 1.");
                    et_quickset_continuous_scan_repeat.setText("2");
                }
            }
            return false;
        }
    };


    /**
     * Send broadcast  ACTION_WRITE_SCAN_CONFIG will  through WriteScanConfigStatusReceiver  to get  the status of set config to the device(SetConfig should be called)
     */
    public class WriteScanConfigStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.GONE);
            byte status[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS);
            btn_scan.setClickable(true);
            if((int)status[0] == 1)
            {
                if((int)status[2] == -1 && (int)status[3]==-1)
                {
                    Dialog_Pane("Fail","Set configuration fail!");
                }
                else
                {
                    //Get the scan config of the device
                    ISCNIRScanSDK.ReadCurrentScanConfig();
                }
            }
            else if((int)status[0] == -1)
            {
                Dialog_Pane("Fail","Set configuration fail!");
            }
            else if((int)status[0] == -2)
            {
                Dialog_Pane("Fail","Set configuration fail! Hardware not compatible!");
            }
            else if((int)status[0] == -3)
            {
                Dialog_Pane("Fail","Set configuration fail! Function is currently locked!" );
            }
        }
    }

    /**
     * Get  the  current scan config  in the device(ISCNIRScanSDK.ReadCurrentScanConfig(data) should be called)
     */
    public class ReturnCurrentScanConfigurationDataReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Boolean flag = Compareconfig(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_CURRENT_CONFIG_DATA));
            calProgress.setVisibility(View.GONE);
            if(flag)
            {
                if(saveReference == true)
                {
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else if(Current_Scan_Method == ScanMethod.Maintain) //reference
                {
                    ReferenceConfigSaveSuccess();
                }
                else
                {
                    Dialog_Pane("Success","Complete to set configuration.");
                }
            }
            else
            {
                if(saveReference == true)
                {
                    Dialog_Pane("Fail","Restore config fail, should re-open device.");
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else
                {
                    Dialog_Pane("Fail","Set configuration fail.");
                }
            }
        }
    }
    //endregion
    //region Manual UI componet Event
    private Button.OnClickListener Toggle_Button_Manual_ScanMode_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggle_button_manual_scan_mode.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setEnabled(true);
                et_manual_repead.setEnabled(true);
                et_manual_pga.setEnabled(true);
                et_manual_lamptime.setEnabled(false);
                toggle_button_manual_lamp.setChecked(true);
            }
            else
            {
                toggle_button_manual_lamp.setEnabled(false);
                et_manual_repead.setEnabled(false);
                et_manual_pga.setEnabled(false);
                et_manual_lamptime.setEnabled(true);
                toggle_button_manual_lamp.setChecked(false);
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            }
        }
    };
    private ToggleButton.OnCheckedChangeListener Toggle_Button_Manual_Lamp_Changed = new ToggleButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("OFF"))//OFF->ON
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
            }
            else
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.OFF);
            }
        }
    };
    private EditText.OnEditorActionListener Manual_Lamptime_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(Integer.parseInt(et_manual_lamptime.getText().toString())!=625)
                {
                    int lamptime = Integer.parseInt(et_manual_lamptime.getText().toString());
                    ISCNIRScanSDK.SetLampStableTime(lamptime);
                }
                return false;
            }
            return false;
        }
    };

    private EditText.OnEditorActionListener Manual_PGA_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if( checkValidPga()==true)
                {
                    int pga = Integer.parseInt(et_manual_pga.getText().toString());
                    ISCNIRScanSDK.SetPGA(pga);
                    return false;
                }
            }
            return false;
        }
    };

    private EditText.OnEditorActionListener Manual_Repeat_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(checkValidRepeat())
                {
                    int scan_repeat = Integer.parseInt(et_manual_repead.getText().toString());
                    ISCNIRScanSDK.SetScanRepeat(scan_repeat);
                    return false;
                }

            }
            return false;
        }
    };
    private LinearLayout.OnClickListener Manual_Config_Click = new LinearLayout.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    private Button.OnClickListener QuickSet_Continuous_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        }
    };
    //endregion
    //region Scan
    private Button.OnClickListener Button_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            // 202010207 zhaozz:扫描时控制扫描按钮的状态
            setScanBtnStatus(false, getResources().getString(R.string.text_execing));
            setReportBtnStatus(false);
            storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
            long delaytime = 300;
            if(Current_Scan_Method == ScanMethod.Manual)
            {
                if( checkValidPga()==false)
                {
                    NotValidValueDialog("Error","PGA vlaue is 1,2,4,8,16,32,64.");
                    return;
                }
                else if( checkValidRepeat()==false)
                {
                    NotValidValueDialog("Error","Scan repeat range is 1~50.");
                    return;
                }
                else if(toggle_button_manual_scan_mode.getText().toString().equals("On"))
                {
                    DisableAllComponent();
                    setScanBtnStatus(false, getResources().getString(R.string.text_execing));
                    calProgress.setVisibility(View.VISIBLE);
                    PerformScan(delaytime);
                }
                else
                {
                    PerformScan(delaytime);
                }
            }
            else if(Current_Scan_Method == ScanMethod.QuickSet)
            {
                if(toggle_btn_quickset_continuous_scan_mode.isChecked())
                {
                    progressBarinsideText.setVisibility(View.VISIBLE);
                    continuous_count = 1;
                    progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count));
                    continuous_count = 1;
                }
                PerformScan(delaytime);
            }
            else if(Current_Scan_Method == ScanMethod.Maintain)
            {
                if(Toggle_Button_maintain_reference.isChecked())
                {
                    Dialog_Pane_maintain("Warning","Replace Factory Reference is ON !!! \n This sacn result will REPLACE the Factory Reference and can NOT be reversed!");
                }
                else
                {
                    PerformScan(delaytime);
                }
            }
            else//Normal
            {
                if(toggle_btn_continuous_scan.isChecked())
                {
                    progressBarinsideText.setVisibility(View.VISIBLE);
                    continuous_count = 1;
                    progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count));
                }
                PerformScan(delaytime);
            }
            //---------------------------------------------------------------------------------------------------
            if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
            {
            }
            else
            {
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                setScanBtnStatus(false, getResources().getString(R.string.text_execing));
            }
        }
    };

    // 20210207 zhaozz: 预测方法
    private void processPredictRequest()
    {
        setScanBtnStatus(false, getResources().getString(R.string.text_predicting));
        // 20210208 zhaozz:尝试在预测过程中显示等待动画，但是显示不出，有待寻找原因，若还不能，可删除
        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        calProgress.setVisibility(View.VISIBLE);
        progressBarinsideText.setVisibility(View.VISIBLE);
        btn_report.setClickable(false);
        //20210207 zhaozz: 增加上传测试代码
        String url = "http://211.82.95.146:5005/predict";
        Predict predict = new Predict();
        //20210207 zhaozz:获取类别控件的值
        et_quickset_lamptime = (EditText)findViewById(R.id.et_prefix_lamp_quickset);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        String predict_type = spinner.getSelectedItem().toString();
        predict.setCategory(predict_type);
        predict.setAbsorbance(absorbanceList.toArray());
        Intent intent=getIntent();
        String token=intent.getStringExtra("token");
        String json =  JSON.toJSONString(predict);
        Call call = OkHttpClientUtil.postJSON(url,json,token);
        isReqSuccess = false;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("TAG", "onFailure: ");
                Looper.prepare();
                Toast.makeText(ScanViewActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                Looper.loop();
                setScanBtnStatus(false, getResources().getString(R.string.text_scanpredict));
                setReportBtnStatus(false);
                calProgress.setVisibility(View.GONE);
                progressBarinsideText.setVisibility(View.GONE);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code()  == 200) {
                    predictRetVal = response.body().string();
                    isReqSuccess = true;
                    Message msg = Message.obtain();
                    msg.what = 1;
                    handler.sendMessage(msg);
                }
                else{
                    Looper.prepare();
                    Toast.makeText(ScanViewActivity.this, "请求失败："+ response.body().string(), Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    Message msg = Message.obtain();
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            }
        });
    }


    // 定义子线程操作UI的委托
    private Handler handler= new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    setReportBtnStatus(true);
                    setScanBtnStatus(true, getResources().getString(R.string.text_scanpredict));
                    calProgress.setVisibility(View.GONE);
                    progressBarinsideText.setVisibility(View.GONE);
                    break;
                case 2:
                    setReportBtnStatus(false);
                    setScanBtnStatus(true, getResources().getString(R.string.text_scanpredict));
                    calProgress.setVisibility(View.GONE);
                    progressBarinsideText.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    // 20210207 zhaozz: 设置report按钮的状态
    private void setReportBtnStatus(boolean flag) {
        if (flag) {
            btn_report.setClickable(true);
            btn_report.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        } else {
            btn_report.setClickable(false);
            btn_report.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
        }
        btn_report.setText(getResources().getString(R.string.text_report));
    }

    // 20210207 zhaozz: 设置scan按钮的状态, 此处代码有点重复，因为还不会用引用传值
    private void setScanBtnStatus(boolean flag, String content){
        if (flag) {
            btn_scan.setClickable(true);
            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        } else {
            btn_scan.setClickable(false);
            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
        }
        btn_scan.setText(content);
    }

    //点击查看报告按钮的响应事件
    private Button.OnClickListener Button_Scan_Report_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(isReqSuccess) {
                showDialog(predictRetVal);
            }
        }
    };

    //20210207 zhaozz： 定义标记位，确认是否预测成功
    private boolean isReqSuccess = false;

    /**
     * %%发送广播开始扫描将通过ScanStartedReceiver通知扫描（应调用PerformScan）
     * Send broadcast  START_SCAN will  through ScanStartedReceiver  to notify scanning(PerformScan should be called)
     */
    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.VISIBLE);
            btn_scan.setText(getResources().getString(R.string.text_execing));
        }
    }
    boolean continuous = false;
    ISCNIRScanSDK.ReferenceCalibration reference_calibration;
    String CurrentTime;
    long MesureScanTime=0;
    /**
     * Custom receiver for handling scan data and setting up the graphs properly(ISCNIRScanSDK.StartScan() should be called)
     */
    public class ScanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            long endtime = System.currentTimeMillis();
            MesureScanTime = endtime - ISCNIRScanSDK.startScanTime;
            reference_calibration = ISCNIRScanSDK.ReferenceCalibration.currentCalibration.get(0);
            if(Interpret_length<=0)
            {
                Dialog_Pane_Finish("Error","The scan interpret fail. Please check your device.");
            }
            else
            {
                //Get scan spectrum data
                Scan_Spectrum_Data = new ISCNIRScanSDK.ScanResults(Interpret_wavelength,Interpret_intensity,Interpret_uncalibratedIntensity,Interpret_length);

                mXValues.clear();
                mIntensityFloat.clear();
                mAbsorbanceFloat.clear();
                mReflectanceFloat.clear();
                mWavelengthFloat.clear();
                mReferenceFloat.clear();
                int index;
                for (index = 0; index < Scan_Spectrum_Data.getLength(); index++) {
                    mXValues.add(String.format("%.02f", ISCNIRScanSDK.ScanResults.getSpatialFreq(mContext, Scan_Spectrum_Data.getWavelength()[index])));
                    mIntensityFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index]));
                    mAbsorbanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(-1) * (float) Math.log10((double) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / (double) Scan_Spectrum_Data.getIntensity()[index])));
                    mReflectanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / Scan_Spectrum_Data.getIntensity()[index]));
                    mWavelengthFloat.add((float) Scan_Spectrum_Data.getWavelength()[index]);
                    mReferenceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getIntensity()[index]));
                }
                minWavelength = mWavelengthFloat.get(0);
                maxWavelength = mWavelengthFloat.get(0);

                for (Float f : mWavelengthFloat) {
                    if (f < minWavelength) minWavelength = f;
                    if (f > maxWavelength) maxWavelength = f;
                }
                minAbsorbance = mAbsorbanceFloat.get(0).getY();
                maxAbsorbance = mAbsorbanceFloat.get(0).getY();
                for (Entry e : mAbsorbanceFloat) {
                    if (e.getY() < minAbsorbance || Float.isNaN(minAbsorbance)) minAbsorbance = e.getY();
                    if (e.getY() > maxAbsorbance || Float.isNaN(maxAbsorbance)) maxAbsorbance = e.getY();
                }
                if(minAbsorbance==0 && maxAbsorbance==0)
                {
                    maxAbsorbance=2;
                }
                minReflectance = mReflectanceFloat.get(0).getY();
                maxReflectance = mReflectanceFloat.get(0).getY();

                for (Entry e : mReflectanceFloat) {
                    if (e.getY() < minReflectance|| Float.isNaN(minReflectance) ) minReflectance = e.getY();
                    if (e.getY() > maxReflectance|| Float.isNaN(maxReflectance) ) maxReflectance = e.getY();
                }
                if(minReflectance==0 && maxReflectance==0)
                {
                    maxReflectance=2;
                }
                minIntensity = mIntensityFloat.get(0).getY();
                maxIntensity = mIntensityFloat.get(0).getY();

                for (Entry e : mIntensityFloat) {
                    if (e.getY() < minIntensity|| Float.isNaN(minIntensity)) minIntensity = e.getY();
                    if (e.getY() > maxIntensity|| Float.isNaN(maxIntensity)) maxIntensity = e.getY();
                }
                if(minIntensity==0 && maxIntensity==0)
                {
                    maxIntensity=1000;
                }
                minReference = mReferenceFloat.get(0).getY();
                maxReference = mReferenceFloat.get(0).getY();

                for (Entry e : mReferenceFloat) {
                    if (e.getY() < minReference || Float.isNaN(minReference)) minReference = e.getY();
                    if (e.getY() > maxReference || Float.isNaN(maxReference)) maxReference = e.getY();
                }
                if(minReference==0 && maxReference==0)
                {
                    maxReference=1000;
                }
                isScan = true;
                tabPosition = mViewPager.getCurrentItem();
                mViewPager.setAdapter(mViewPager.getAdapter());
                mViewPager.invalidate();
                //number of slew
                String slew="";
                if(activeConf != null && activeConf.getScanType().equals("Slew")){
                    int numSections = activeConf.getSlewNumSections();
                    int i;
                    for(i = 0; i < numSections; i++){
                        slew = slew + activeConf.getSectionNumPatterns()[i]+"%";
                    }
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                SimpleDateFormat filesimpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
                String ts = simpleDateFormat.format(new Date());
                CurrentTime = filesimpleDateFormat.format(new Date());
                ActionBar ab = getActionBar();
                if (ab != null) {
                    if (filePrefix.getText().toString().equals("")) {
                        ab.setTitle("扫描时间：" + ts);
                    } else {
                        ab.setTitle(filePrefix.getText().toString() + ts);
                    }
                    ab.setSelectedNavigationItem(0);
                }
                if(Current_Scan_Method == ScanMethod.Normal)
                {
                    continuous = toggle_btn_continuous_scan.isChecked();
                }
                else
                {
                    continuous = toggle_btn_quickset_continuous_scan_mode.isChecked();
                }
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
                //Get Device information from the device
                ISCNIRScanSDK.GetDeviceStatus();
            }
        }
    }
    /**
     * GetDeviceStatusReceiver to get  the device status(ISCNIRScanSDK.GetDeviceStatus()should be called)
     */
    String battery="";
    String TotalLampTime;
    byte[] devbyte;
    byte[] errbyte;
    public class GetDeviceStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            battery = Integer.toString( intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0));
            long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME,0);
            TotalLampTime = GetLampTimeString(lamptime);
            devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
            errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
            if((isExtendVer && fw_level.compareTo(FW_LEVEL.LEVEL_EXT_2)>=0) || (!isExtendVer &&  fw_level.compareTo(FW_LEVEL.LEVEL_3)>=0))
                ISCNIRScanSDK.GetScanLampRampUpADC();
            else
                DoScanComplete();
        }
    }
    /**

     *Get lamp ramp up adc data (ISCNIRScanSDK.GetScanLampRampUpADC() should be called)
     */
    private byte Lamp_RAMPUP_ADC_DATA[];
    public class ReturnLampRampUpADCReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_RAMPUP_DATA);
            ISCNIRScanSDK.GetLampADCAverage();
        }
    }
    /**
     *Get lamp average adc data (ISCNIRScanSDK.GetLampADCAverage() should be called)
     */
    private byte Lamp_AVERAGE_ADC_DATA[];
    public class ReturnLampADCAverageReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_AVERAGE_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_AVERAGE_DATA);
            DoScanComplete();
        }
    }
    /**
     * %%完成扫描会将扫描数据写入.csv和设置界面
     * 连续扫描将触发扫描事件来扫描数据
     * Finish scan will write scan data to .csv and setting UI
     * Continuous scan will trigger scan event to scan data
     */
    private void DoScanComplete()
    {
        long delaytime =0;
        Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,true);
        if(isLockButton) //User open lock button on scan setting
        {
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
            delaytime = 300;
        }
        if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") ==false)
        {
            closeFunction();
        }
        writeCSV( Scan_Spectrum_Data);
        //------------------------------------------------------------------------------------------------------------
        calProgress.setVisibility(View.GONE);
        progressBarinsideText.setVisibility(View.GONE);
        EnableAllComponent();
        Disable_Stop_Continous_button();
        //Tiva version <2.1.0.67
        if(fw_level.compareTo(FW_LEVEL.LEVEL_0)==0)
            closeFunction();
        //-------------------------------------------------------------------------------------------------------------
        if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
        {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    ISCNIRScanSDK.SaveReference();
                    SaveReferenceDialog();
                }}, 200);
        }
        float interval_time = 0;
        int repeat = 0;
        if(Current_Scan_Method == ScanMethod.Normal)
        {
            interval_time = Float.parseFloat(et_normal_interval_time.getText().toString());
            repeat = Integer.parseInt(et_normal_scan_repeat.getText().toString()) -1;//-1 want to match scan count
        }
        else//Quick set mode
        {
            interval_time = Integer.parseInt(et_quickset_scan_interval_time.getText().toString());
            repeat = Integer.parseInt(et_quickset_continuous_scan_repeat.getText().toString()) -1;//-1 want to match scan count
        }
        if (continuous) {
            progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count +1));
            if(continuous_count == repeat +1 || stop_continuous == true)
            {
                continuous = false;
                stop_continuous = false;
                toggle_btn_quickset_continuous_scan_mode.setChecked(false);
                toggle_btn_continuous_scan.setChecked(false);
                Disable_Stop_Continous_button();
                String content = "There were totally " + continuous_count + " scans has been performed!.";
                Dialog_Pane("Continuous Scan Completed!",content);
                continuous_count = 0;
                progressBarinsideText.setVisibility(View.GONE);
                return;
            }
            continuous_count ++;
            calProgress.setVisibility(View.VISIBLE);
            progressBarinsideText.setVisibility(View.VISIBLE);
            DisableAllComponent();
            Enable_Stop_Continous_button();
            try {
                Thread.sleep((long) (interval_time*1000));
            }catch (Exception e)
            {

            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    ISCNIRScanSDK.StartScan();
                }}, delaytime);

        }
    }
    /**
     * %%将扫描数据写入CSV文件
     * Write scan data to CSV file
     * @param scanResults the {@link ISCNIRScanSDK.ScanResults} structure to save
     */
    private void writeCSV(ISCNIRScanSDK.ScanResults scanResults) {
        //2021 zhaozz： 扫描时，设置检测接口不可用
        btn_report.setClickable(false);
        int scanType;
        int numSections;
        String widthnm[] ={"","","2.34","3.51","4.68","5.85","7.03","8.20","9.37","10.54","11.71","12.88","14.05","15.22","16.39","17.56","18.74"
                ,"19.91","21.08","22.25","23.42","24.59","25.76","26.93","28.10","29.27","30.44","31.62","32.79","33.96","35.13","36.30","37.47","38.64","39.81"
                ,"40.98","42.15","43.33","44.50","45.67","46.84","48.01","49.18","50.35","51.52","52.69","53.86","55.04","56.21","57.38","58.55","59.72","60.89"};
        String exposureTime[] = {"0.635","1.27"," 2.54"," 5.08","15.24","30.48","60.96"};
        int index = 0;
        double temp;
        double humidity;
        //-------------------------------------------------
        String newdate = "";
        String CSV[][] = new String[35][15];
        for (int i = 0; i < 35; i++)
            for (int j = 0; j < 15; j++)
                CSV[i][j] = ",";

        numSections = Scan_Config_Info.numSections[0];
        scanType = Scan_Config_Info.scanType[0];
        //----------------------------------------------------------------
        String configname = getBytetoString(Scan_Config_Info.configName);
        if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
        {
            configname = "Reference";
            Date datetime = new Date();
            SimpleDateFormat format_1 = new SimpleDateFormat("yy/mm/dd");
            SimpleDateFormat format_2 = new SimpleDateFormat("HH:mm:ss");
            newdate = format_1.format(datetime) + "T" + format_2.format(datetime);
            CSV[14][8] = newdate;
        }
        else
        {
            CSV[14][8] =  Reference_Info.refday[0]  + "/" +Reference_Info.refday[1] + "/"+ Reference_Info.refday[2] + "T" + Reference_Info.refday[3] + ":" + Reference_Info.refday[4] + ":" + Reference_Info.refday[5];
        }
        String prefix = filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "ISC";
        }
        if(android.os.Environment.getExternalStorageState().equals(android.os. Environment.MEDIA_REMOVED))
        {
            Toast.makeText(ScanViewActivity.this , "No SD card." , Toast.LENGTH_SHORT ).show();
            return ;
        }
        //--------------------------------------
        File mSDFile  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report");
        //No file exist
        if(!mFile.exists())
        {
            mFile.mkdirs();
        }
        mFile.setExecutable(true);
        mFile.setReadable(true);
        mFile.setWritable(true);

        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        MediaScannerConnection.scanFile(this, new String[] {mFile.toString()}, null, null);
        //-------------------------------------------------------------------------
        String csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix+"_" + configname + "_" + CurrentTime + ".csv";
        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        MediaScannerConnection.scanFile(this, new String[] {csvOS}, null, null);
        // Section information field names
        CSV[0][0] = "***Scan Config Information***,";
        CSV[1][0] = "Scan Config Name:,";
        CSV[2][0] = "Scan Config Type:,";
        CSV[3][0] = "Section Config Type:,";
        CSV[4][0] = "Start Wavelength (nm):,";
        CSV[5][0] = "End Wavelength (nm):,";
        CSV[6][0] = "Pattern Width (nm):,";
        CSV[7][0] = "Exposure (ms):,";
        CSV[8][0] = "Digital Resolution:,";
        CSV[9][0] = "Num Repeats:,";
        CSV[10][0] = "PGA Gain:,";
        CSV[11][0] = "System Temp (C):,";
        CSV[12][0] = "Humidity (%):,";
        CSV[13][0] = "Battery Capacity (%):,";
        if((isExtendVer && fw_level.compareTo(FW_LEVEL.LEVEL_EXT_2)>=0) || (!isExtendVer &&  fw_level.compareTo(FW_LEVEL.LEVEL_3)>=0))
            CSV[14][0] = "Lamp ADC:,";
        else
            CSV[14][0] = "Lamp Intensity:,";
        CSV[15][0] = "Data Date-Time:,";
        CSV[16][0] = "Total Measurement Time in sec:,";

        CSV[1][1] = configname  + ",";
        CSV[2][1] = "Slew,";
        CSV[2][2] = "Num Section:,";
        CSV[2][3] = Integer.toString(numSections) + ",";

        for(int i=0;i<numSections;i++)
        {
            if(Scan_Config_Info.sectionScanType[i] ==0)
                CSV[3][i+1] = "Column,";
            else
                CSV[3][i+1] = "Hadamard,";
            CSV[4][i+1] = Scan_Config_Info.sectionWavelengthStartNm[i] + ",";
            CSV[5][i+1] = Scan_Config_Info.sectionWavelengthEndNm[i] + ",";
            index = Scan_Config_Info.sectionWidthPx[i];
            CSV[6][i+1] = widthnm[index] + ",";
            index = Scan_Config_Info.sectionExposureTime[i];
            CSV[7][i+1] = exposureTime[index] + ",";
            CSV[8][i+1] = Scan_Config_Info.sectionNumPatterns[i] + ",";
        }
        CSV[9][1] =Scan_Config_Info. sectionNumRepeats[0] + ",";
        CSV[10][1] = Scan_Config_Info.pga[0] + ",";
        temp = Scan_Config_Info.systemp[0];
        temp = temp/100;
        CSV[11][1] = temp  + ",";
        humidity =  Scan_Config_Info.syshumidity[0];
        humidity =  humidity/100;
        CSV[12][1] = humidity  + ",";
        CSV[13][1] = battery + ",";
        CSV[14][1] = Scan_Config_Info.lampintensity[0] + ",";
        CSV[15][1] = Scan_Config_Info.day[0] + "/" + Scan_Config_Info.day[1] + "/"+ Scan_Config_Info.day[2]  + "T" + Scan_Config_Info.day[3] + ":" + Scan_Config_Info.day[4] + ":" + Scan_Config_Info.day[5] + ",";
        CSV[16][1] = Double.toString((double) MesureScanTime/1000);
        //General Information
        CSV[17][0] = "***General Information***,";
        CSV[18][0] = "Model Name:,";
        CSV[19][0] = "Serial Number:,";
        CSV[20][0] = "APP Version:,";
        CSV[21][0] = "TIVA Version:,";
        //CSV[22][0] = "DLPC Version:,";
        CSV[22][0] = "UUID:,";
        CSV[23][0] = "Main Board Version:,";
        CSV[24][0] = "Detector Board Version:,";

        CSV[18][1] = model_name + ",";
        CSV[19][1] = serial_num + ",";
        String mfg_num = "";
        try {
             mfg_num  = new String(MFG_NUM, "ISO-8859-1");
            if (!mfg_num.contains("70UB1") && !mfg_num.contains("95UB1"))
                mfg_num = "";
            else if(mfg_num.contains("95UB1"))
                mfg_num = mfg_num.substring(0,mfg_num.length()-2);
        }catch (Exception e)
        {
            mfg_num = "";
        };
        CSV[19][2] = mfg_num + ",";
        String version = "";
        int versionCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        CSV[20][1] = version + "." + Integer.toString(versionCode);
        CSV[21][1] = Tivarev + ",";
        CSV[22][1] = uuid + ",";
        String split_hw[] = HWrev.split("\\.");
        CSV[23][1] = split_hw[0] + ",";
        CSV[24][1] = split_hw[2] + ",";

        //Reference Scan Information
        CSV[0][7] = "***Reference Scan Information***,";
        CSV[1][7] = "Scan Config Name:,";
        CSV[2][7] = "Scan Config Type:,";
        CSV[3][7] = "Section Config Type:,";
        CSV[4][7] = "Start Wavelength (nm):,";
        CSV[5][7] = "End Wavelength (nm):,";
        CSV[6][7] = "Pattern Width (nm):,";
        CSV[7][7] = "Exposure (ms):,";
        CSV[8][7] = "Digital Resolution:,";
        CSV[9][7] = "Num Repeats:,";
        CSV[10][7] = "PGA Gain:,";
        CSV[11][7] = "System Temp (C):,";
        CSV[12][7] = "Humidity (%):,";
        CSV[13][7] = "Lamp Intensity:,";
        CSV[14][7] = "Data Date-Time:,";
        if(getBytetoString(Reference_Info.refconfigName).equals("SystemTest"))
            CSV[1][8] = "Built-in Factory Reference";
        else
            CSV[1][8] = "Built-in User Reference";
        CSV[2][8] = "Slew,";
        CSV[2][9] = "Num Section:,";
        CSV[2][10] = "1,";
        if(Reference_Info.refconfigtype[0] == 0)
            CSV[3][8] = "Column,";
        else
            CSV[3][8] = "Hadamard,";
        CSV[4][8] = Double.toString(Reference_Info.refstartwav[0]);
        CSV[5][8] = Double.toString(Reference_Info.refendwav[0]);
        index = Reference_Info.width[0];
        CSV[6][8] = widthnm[index] + ",";
        index = Reference_Info.refexposuretime[0];
        CSV[7][8] = exposureTime[index] + ",";
        CSV[8][8] = Integer.toString( Reference_Info.numpattren[0]) + ",";
        CSV[9][8] = Reference_Info.numrepeat[0] + ",";
        CSV[10][8] = Integer.toString(Reference_Info.refpga[0]);
        temp = Reference_Info.refsystemp[0];
        temp = temp/100;
        CSV[11][8] = Double.toString(temp) + ",";
        humidity =  Reference_Info.refsyshumidity[0]/100;
        CSV[12][8] = Double.toString(humidity) ;
        CSV[13][8] = Reference_Info.reflampintensity[0] +",";

        //Calibration Coefficients
        CSV[17][7] = "***Calibration Coefficients***,";
        CSV[18][7] = "Shift Vector Coefficients:,";
        CSV[19][7] = "Pixel to Wavelength Coefficients:,";
        CSV[21][7] = "***Lamp Usage * **,";
        CSV[22][7] ="Total Time(HH:MM:SS):,";
        CSV[23][7] ="***Device/Error Status***,";
        CSV[24][7] ="Device Status:,";
        CSV[25][7] ="Error status:,";

        CSV[18][8] = Scan_Config_Info.shift_vector_coff[0] + ",";
        CSV[18][9] = Scan_Config_Info.shift_vector_coff[1] + ",";
        CSV[18][10] = Scan_Config_Info.shift_vector_coff[2] + ",";

        CSV[19][8] = Scan_Config_Info.pixel_coff[0] + ",";
        CSV[19][9] = Scan_Config_Info.pixel_coff[1] + ",";
        CSV[19][10] = Scan_Config_Info.pixel_coff[2] + ",";
        CSV[22][8] = TotalLampTime + ",";
        final StringBuilder stringBuilder = new StringBuilder(8);
        for(int i= 3;i>= 0;i--)
            stringBuilder.append(String.format("%02X", devbyte[i]));
        CSV[24][8] ="0x" + stringBuilder.toString();
        final StringBuilder stringBuilder_errorstatus = new StringBuilder(8);
        for(int i= 3;i>= 0;i--)
            stringBuilder_errorstatus.append(String.format("%02X", errbyte[i]));
        CSV[25][8] ="0x" + stringBuilder_errorstatus.toString() + ",";
        final StringBuilder stringBuilder_errorcode = new StringBuilder(8);
        for(int i= 4;i<20;i+=2)
        {
            stringBuilder_errorcode.append(String.format("%02X", errbyte[i+1]));
            stringBuilder_errorcode.append(String.format("%02X", errbyte[i]));
        }

        CSV[25][9] = "Error Code:,";
        CSV[25][10] ="0x" + stringBuilder_errorcode.toString() + ",";
        CSV[27][0] = "***Scan Data***,";
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(csvOS), ',', CSVWriter.NO_QUOTE_CHARACTER);
            List<String[]> data = new ArrayList<String[]>();

            //20210207 zhaozz: 增加存储absorbance的代码
           absorbanceList = new ArrayList<Double>();

            String buf = "";
            for (int i = 0; i < 28; i++)
            {
                for (int j = 0; j < 15; j++)
                {
                    buf += CSV[i][j];
                    if (j == 14)
                    {
                        data.add(new String[]{buf});
                    }
                }
                buf = "";
            }
            data.add(new String[]{"Wavelength (nm),Absorbance (AU),Reference Signal (unitless),Sample Signal (unitless)"});
            int csvIndex;
            for (csvIndex = 0; csvIndex < scanResults.getLength(); csvIndex++) {
                double waves = scanResults.getWavelength()[csvIndex];
                int intens = scanResults.getUncalibratedIntensity()[csvIndex];
                float absorb = (-1) * (float) Math.log10((double) scanResults.getUncalibratedIntensity()[csvIndex] / (double) scanResults.getIntensity()[csvIndex]);
                //float reflect = (float) Scan_Spectrum_Data.getUncalibratedIntensity()[csvIndex] / Scan_Spectrum_Data.getIntensity()[csvIndex];
                float reference = (float) Scan_Spectrum_Data.getIntensity()[csvIndex];
                data.add(new String[]{String.valueOf(waves), String.valueOf(absorb),String.valueOf(reference), String.valueOf(intens)});
                //20210207 zhaozz: 增加存储absorbance的代码
                if(csvIndex>=10 && csvIndex<210){
                    double absorb_mat = (-1) * (double) Math.log10((double) scanResults.getUncalibratedIntensity()[csvIndex] / (double) scanResults.getIntensity()[csvIndex]);
                    absorbanceList.add(absorb_mat);
                }
            }
            if((isExtendVer && fw_level.compareTo(FW_LEVEL.LEVEL_EXT_2)>=0) || (!isExtendVer &&  fw_level.compareTo(FW_LEVEL.LEVEL_3)>=0))
            {
                data.add(new String[]{""});
                data.add(new String[]{"***Lamp Ramp Up ADC***,"});
                data.add(new String[]{"ADC0,ADC1,ADC2,ADC3"});
                String[] ADC = new String[4];
                int count = 0;
                for(int i=0;i<Lamp_RAMPUP_ADC_DATA.length;i+=2)
                {
                    int adc_value = (Lamp_RAMPUP_ADC_DATA[i+1]&0xff)<<8|Lamp_RAMPUP_ADC_DATA[i]&0xff;
                    if(adc_value ==0 && i%8==0 && i/8>0)
                        break;
                    ADC[count] = Integer.toString(adc_value) ;
                    count ++;
                    if(count ==4)
                    {
                        data.add(ADC);
                        count = 0;
                        ADC = new String[4];
                    }
                }
                //-----------------------------------
                data.add(new String[]{""});
                data.add(new String[]{"***Lamp ADC among repeated times***,"});
                data.add(new String[]{"ADC0,ADC1,ADC2,ADC3"});
                ADC = new String[4];
                int Average_ADC[] = new int[4];
                int cal_count =0;
                count = 0;
                for(int i=0;i<Lamp_AVERAGE_ADC_DATA.length;i+=2)
                {
                    int adc_value = (Lamp_AVERAGE_ADC_DATA[i+1]&0xff)<<8|Lamp_AVERAGE_ADC_DATA[i]&0xff;
                    if(adc_value ==0 && i%8==0 && i/8>0)
                        break;
                    ADC[count] =Integer.toString(adc_value) ;
                    Average_ADC[count] +=adc_value;
                    count ++;
                    if(count ==4)
                    {
                        data.add(ADC);
                        cal_count ++;
                        count = 0;
                        ADC = new String[4];
                    }
                }
                String AverageADC = "Lamp ADC:,";

                for(int i=0;i<4;i++)
                {
                    double buf_adc = (double)Average_ADC[i];
                    AverageADC +=Math.round( buf_adc/cal_count) + ",";
                }
                AverageADC +=",," + CSV[14][7] + CSV[14][8];// add ref data-time data
                data.get(14)[0] = AverageADC;
            }
            Log.d("data","1231321");

            writer.writeAll(data);
            writer.close();

            //2021 zhaozz： 扫描完成后执行预测过程
            processPredictRequest();
        } catch (IOException e) {
            e.printStackTrace();
            setScanBtnStatus(true, getResources().getString(R.string.text_scanpredict));
            setReportBtnStatus(false);
        }
    }
    //endregion
    //region Draw spectral plot
    /**
     * Pager enum to control tab tile and layout resource
     */
    public enum CustomPagerEnum {
        //20210225 zhaozz: 此处调整了枚举顺序，为了让chart默认显示吸光度
        ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance),
        REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance),
        INTENSITY(R.string.intensity, R.layout.page_graph_intensity),
        REFERENCE(R.string.reference_tab,R.layout.page_graph_reference);
        private final int mTitleResId;
        private final int mLayoutResId;
        CustomPagerEnum(int titleResId, int layoutResId) {
            mTitleResId = titleResId;
            mLayoutResId = layoutResId;
        }
        public int getLayoutResId() {
            return mLayoutResId;
        }
    }

    /**
     * Custom pager adapter to handle changing chart data when pager tabs are changed
     */
    public class CustomPagerAdapter extends PagerAdapter {

        private final Context mContext;

        public CustomPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
            collection.addView(layout);

            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartInt);
                mChart.setDrawGridBackground(false);

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(true);
                leftAxis.setAxisMaximum(maxIntensity);
                leftAxis.setAxisMinimum(minIntensity);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);

                // add data

                if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }
                if(numSections>=2 &&(Float.isNaN(minIntensity)==false && Float.isNaN(maxIntensity)==false) && Current_Scan_Method!= ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mIntensityFloat,numSections); //scan data section > 1
                }
                else if(Float.isNaN(minIntensity)==false && Float.isNaN(maxIntensity)==false)
                {
                    setData(mChart, mXValues, mIntensityFloat,ChartType.INTENSITY);//scan data section = 1
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartAbs);
                mChart.setDrawGridBackground(false);

                // enable touch gestures zzzzzzzzzzzzzz
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(false);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxAbsorbance);
                leftAxis.setAxisMinimum(minAbsorbance);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections=0;
                if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }
                if(numSections>=2 &&(Float.isNaN(minAbsorbance)==false && Float.isNaN(maxAbsorbance)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)////Scan method : quickset only one section
                {
                    setDataSlew(mChart, mAbsorbanceFloat,numSections);
                }
                else if( Float.isNaN(minAbsorbance)==false && Float.isNaN(maxAbsorbance)==false)
                {
                    setData(mChart, mXValues, mAbsorbanceFloat, ChartType.ABSORBANCE);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);

                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reflectance) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartRef);
                mChart.setDrawGridBackground(false);


                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxReflectance);
                leftAxis.setAxisMinimum(minReflectance);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections=0;
                if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }
                if(numSections>=2 &&(Float.isNaN(minReflectance)==false && Float.isNaN(maxReflectance)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReflectanceFloat,numSections);
                }

                else if(Float.isNaN(minReflectance)==false && Float.isNaN(maxReflectance)==false)
                {
                    setData(mChart, mXValues, mReflectanceFloat, ChartType.REFLECTANCE);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reference) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartReference);
                mChart.setDrawGridBackground(false);

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxReference);
                leftAxis.setAxisMinimum(minReference);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections=0;
                if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }
                if(numSections>=2 &&(Float.isNaN(minReference)==false && Float.isNaN(maxReference)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReferenceFloat,numSections);
                }
                else if( Float.isNaN(minReference)==false && Float.isNaN(maxReference)==false)
                {
                    setData(mChart, mXValues, mReferenceFloat, ChartType.INTENSITY);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);

                return layout;
            }else {
                return layout;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return CustomPagerEnum.values().length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.reflectance);
                case 1:
                    return getString(R.string.absorbance);
                case 2:
                    return getString(R.string.intensity);
            }
            return null;
        }

    }

    /**
     *%%
     */

    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {

        if (type == ChartType.REFLECTANCE) {
            //init yvalues
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            //---------------------------------------------------------
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues,GraphLabel);
            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.RED);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.RED);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.ABSORBANCE) {
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.GREEN);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.GREEN);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLUE);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);


            mChart.setMaxVisibleValueCount(20);
        } else {
            int size = yValues.size();
            if(size == 0)
            {
                yValues.add(new Entry((float) -10, (float) -10));
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLACK);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(10);
        }
    }

    private void setDataSlew(LineChart mChart, ArrayList<Entry> yValues,int slewnum)
    {
        if(yValues.size()<=1)
        {
            return;
        }
        ArrayList<Entry> yValues1 = new ArrayList<Entry>();
        ArrayList<Entry> yValues2 = new ArrayList<Entry>();
        ArrayList<Entry> yValues3 = new ArrayList<Entry>();
        ArrayList<Entry> yValues4 = new ArrayList<Entry>();
        ArrayList<Entry> yValues5 = new ArrayList<Entry>();

        for(int i=0;i<activeConf.getSectionNumPatterns()[0];i++)
        {
            if(Float.isInfinite(yValues.get(i).getY()) == false)
            {
                yValues1.add(new Entry(yValues.get(i).getX(),yValues.get(i).getY()));
            }
        }
        int offset = activeConf.getSectionNumPatterns()[0];
        for(int i=0;i<activeConf.getSectionNumPatterns()[1];i++)
        {
            if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
            {
                yValues2.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
            }
        }
        if(slewnum>=3)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1];
            for(int i=0;i<activeConf.getSectionNumPatterns()[2];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues3.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }

            }
        }
        if(slewnum>=4)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2];
            for(int i=0;i<activeConf.getSectionNumPatterns()[3];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues4.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }
            }
        }
        if(slewnum==5)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2]+ activeConf.getSectionNumPatterns()[3];
            for(int i=0;i<activeConf.getSectionNumPatterns()[4];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues5.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }
            }
        }
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yValues1, "Slew1");
        LineDataSet set2 = new LineDataSet(yValues2, "Slew2");
        LineDataSet set3 = new LineDataSet(yValues3, "Slew3");
        LineDataSet set4 = new LineDataSet(yValues4, "Slew4");
        LineDataSet set5 = new LineDataSet(yValues5, "Slew5");

        // set the line to be drawn like this "- - - - - -"
        set1.enableDashedLine(10f, 5f, 0f);
        set1.enableDashedHighlightLine(10f, 5f, 0f);
        set1.setColor(Color.BLUE);
        set1.setCircleColor(Color.BLUE);
        set1.setLineWidth(1f);
        set1.setCircleSize(2f);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.BLUE);
        set1.setDrawFilled(true);
        set1.setValues(yValues1);

        // set the line to be drawn like this "- - - - - -"
        set2.enableDashedLine(10f, 5f, 0f);
        set2.enableDashedHighlightLine(10f, 5f, 0f);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.RED);
        set2.setLineWidth(1f);
        set2.setCircleSize(2f);
        set2.setDrawCircleHole(false);
        set2.setValueTextSize(9f);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.RED);
        set2.setDrawFilled(true);
        set2.setValues(yValues2);
        // set the line to be drawn like this "- - - - - -"
        set3.enableDashedLine(10f, 5f, 0f);
        set3.enableDashedHighlightLine(10f, 5f, 0f);
        set3.setColor(Color.GREEN);
        set3.setCircleColor(Color.GREEN);
        set3.setLineWidth(1f);
        set3.setCircleSize(2f);
        set3.setDrawCircleHole(false);
        set3.setValueTextSize(9f);
        set3.setFillAlpha(65);
        set3.setFillColor(Color.GREEN);
        set3.setDrawFilled(true);
        set3.setValues(yValues3);
        // set the line to be drawn like this "- - - - - -"
        set4.enableDashedLine(10f, 5f, 0f);
        set4.enableDashedHighlightLine(10f, 5f, 0f);
        set4.setColor(Color.YELLOW);
        set4.setCircleColor(Color.YELLOW);
        set4.setLineWidth(1f);
        set4.setCircleSize(2f);
        set4.setDrawCircleHole(false);
        set4.setValueTextSize(9f);
        set4.setFillAlpha(65);
        set4.setFillColor(Color.YELLOW);
        set4.setDrawFilled(true);
        set4.setValues(yValues4);

        // set the line to be drawn like this "- - - - - -"
        set5.enableDashedLine(10f, 5f, 0f);
        set5.enableDashedHighlightLine(10f, 5f, 0f);
        set5.setColor(Color.LTGRAY);
        set5.setCircleColor(Color.LTGRAY);
        set5.setLineWidth(1f);
        set5.setCircleSize(2f);
        set5.setDrawCircleHole(false);
        set5.setValueTextSize(9f);
        set5.setFillAlpha(65);
        set5.setFillColor(Color.LTGRAY);
        set5.setDrawFilled(true);
        set5.setValues(yValues5);

        if(slewnum==2)
        {
            LineData data = new LineData(set1, set2);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
        if(slewnum==3)
        {
            LineData data = new LineData(set1, set2,set3);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==4)
        {
            LineData data = new LineData(set1, set2,set3,set4);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==5)
        {
            LineData data = new LineData(set1, set2,set3,set4,set5);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
    }
    /**
     * Custom enum for chart type
     */
    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }
    //endregion
    //region Common function
    private Boolean checkValidRepeat()
    {
        try
        {
            int value = Integer.parseInt(et_manual_repead.getText().toString());
            if(value>=1&&value<=50)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }

        return false;
    }
    private Boolean checkValidPga()
    {
        try
        {
            int value = Integer.parseInt(et_manual_pga.getText().toString());
            if(value==1 || value == 2 || value == 4 || value==8 || value==16 || value==32 ||value==64)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }
        return false;
    }

    private Boolean checkQuicksetValue()
    {
        if(Integer.parseInt(et_quickset_spec_start.getText().toString())<MINWAV || Integer.parseInt(et_quickset_spec_start.getText().toString())>MAXWAV)
        {
            Dialog_Pane("Error","Spectral Start (nm) range is " + MINWAV + "~" + MAXWAV + ".");
            return false;
        }
        if(Integer.parseInt(et_quickset_spec_end.getText().toString())<MINWAV || Integer.parseInt(et_quickset_spec_end.getText().toString())>MAXWAV)
        {
            Dialog_Pane("Error","Spectral End (nm) range is "  + MINWAV + "~" + MAXWAV + ".");
            return false;
        }
        if(Integer.parseInt(et_quickset_spec_end.getText().toString())<= Integer.parseInt(et_quickset_spec_start.getText().toString()))
        {
            Dialog_Pane("Error","Spectral End (nm) should larger than  Spectral Start (nm).");
            return false;
        }
        if(Integer.parseInt(et_quickset_average_scan.getText().toString())>65535)
        {
            Dialog_Pane("Error","Average Scans (times) range is 0~65535.");
            return false;
        }
        if(Integer.parseInt(et_quickset_res.getText().toString())>MaxPattern || Integer.parseInt(et_quickset_res.getText().toString())<2)
        {
            Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + ".");
            return false;
        }
        return true;
    }
    /*** Filter out all non-numeric, / and-characters***/
    public static String filterDate(String Str) {
        String filter = "[^0-9^A-Z^a-z]"; // Specify the characters to be filtered
        Pattern p = Pattern.compile(filter);
        Matcher m = p.matcher(Str);
        return m.replaceAll("").trim(); // Replace all characters other than those set above
    }
    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //change to rawData length by half
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //Convert hex data to decimal value
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //Shift the binary value of the first value by 4 bits to the left,ex: 00001000 => 10000000 (8=>128)
            //Then concatenate with the binary value of the second value ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //Complementary with FFFFFFFF
            if (value > 127)
                value -= 256;
            //Finally change to byte
            rawData [i] = (byte) value;
        }
        return rawData ;
    }

    /**
     * %%
     */
    public static String getBytetoString(byte configName[]) {
        byte[] byteChars = new byte[40];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] var3 = byteChars;
        int i = byteChars.length;
        for(int var5 = 0; var5 < i; ++var5) {
            byte b = var3[var5];
            byteChars[b] = 0;
        }
        String s = null;
        for(i = 0; i < configName.length; ++i) {
            byteChars[i] = configName[i];
            if(configName[i] == 0) {
                break;
            }
            os.write(configName[i]);
        }
        try {
            s = new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException var7) {
            var7.printStackTrace();
        }

        return s;
    }
    //endregion
    //region Dialog Pane
    private void Dialog_Pane_Finish(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_maintain(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes_i_know), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                calProgress.setVisibility(View.VISIBLE);
                SetReferenceParameter();
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toggle_Button_maintain_reference.setChecked(false);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void ReferenceConfigSaveSuccess() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Complete save reference config, start scan");
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                PerformScan(0);
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.scanning));
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void NotValidValueDialog(String title,String content) {
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
                btn_quickset_set_config.setClickable(true);
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OpenFunction(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                btn_quickset_set_config.setClickable(true);
                openFunction();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OldTIVA(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                NotValidValueDialog("Limited Functions","Running with older Tiva firmware\nis not recommended and functions\nwill be limited!");
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    /**
     * Dialog that tells the user that a Nano is not connected. The activity will finish when the
     * user selects ok
     */
    private void notConnectedDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.not_connected_message));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    Boolean saveReference = false;
    private void SaveReferenceDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Replace Factory Reference is complete.\nShould reconnect bluetooth to reload reference.");

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "ReferenceScan");
                ISCNIRScanSDK.ScanConfig(ActiveConfigByte,ISCNIRScanSDK.ScanConfig.SET);
                saveReference = true;
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
    //endregion
    // back to this page shoule get active config index
    private void GetActiveConfigOnResume()
    {
        ScanConfigList_from_ScanConfiguration = ScanConfigurationsViewActivity.bufconfigs;//from scan configuration
        ScanConfig_Byte_List_from_ScanConfiuration = ScanConfigurationsViewActivity.bufEXTRADATA_fromScanConfigurationsViewActivity;
        int storenum = ScanConfigList_from_ScanConfiguration.size();
        if(storenum !=0)
        {
            if(storenum!=ScanConfigList.size())
            {
                ScanConfigList.clear();
                ScanConfig_Byte_List.clear();
                for(int i=0;i<ScanConfigList_from_ScanConfiguration.size();i++)
                {
                    ScanConfigList.add(ScanConfigList_from_ScanConfiguration.get(i));
                    ScanConfig_Byte_List.add(ScanConfig_Byte_List_from_ScanConfiuration.get(i));
                }
            }
            for(int i=0;i<ScanConfigList.size();i++)
            {
                int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                if(ActiveConfigindex == ScanConfigIndextoByte )
                {
                    activeConf = ScanConfigList.get(i);
                    ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    tv_normal_scan_conf.setText(activeConf.getConfigName());
                    tv_manual_scan_conf.setText(activeConf.getConfigName());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
        numSections=0;
        //From HomeViewActivity to ScanViewActivity
        if(!init_viewpage_valuearray)
        {
            init_viewpage_valuearray = true;
            //Initialize view pager
            CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(this);
            mViewPager.setAdapter(pagerAdapter);
            mViewPager.invalidate();
            mViewPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                            // When swiping between pages, select the
                            // corresponding tab.
                            ActionBar ab = getActionBar();
                            if (ab != null) {
                                getActionBar().setSelectedNavigationItem(position);
                            }
                        }
                    });

            mXValues = new ArrayList<>();
            mIntensityFloat = new ArrayList<>();
            mAbsorbanceFloat = new ArrayList<>();
            mReflectanceFloat = new ArrayList<>();
            mWavelengthFloat = new ArrayList<>();
            mReferenceFloat = new ArrayList<>();
        }
        else
        {
            if(fw_level.compareTo(FW_LEVEL.LEVEL_0)==0 || isOldTiva)
                closeFunction();
            else if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, null).contains("Activated"))
            {
                if(!showActiveconfigpage)
                    openFunction();
            }
            else
                closeFunction();
        }
        if(!showActiveconfigpage)
        {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfSizeReceiver, new IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetActiveScanConfReceiver, new IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(WriteScanConfigStatusReceiver, WriteScanConfigStatusFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetDeviceStatusReceiver,new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));

        }
        //-----------------------------------------------------------------------------------------------------------
        //In active page back to this page,do nothing,don't init scan Configuration text
        if(showActiveconfigpage)
        {
            showActiveconfigpage = false;
        }
        //First to connect
        else
        {
            tv_normal_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
            tv_manual_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
        }
        if(!GotoScanConfigFlag && activeConf!=null)
        {
            tv_normal_scan_conf.setText(activeConf.getConfigName());
            tv_manual_scan_conf.setText(activeConf.getConfigName());
        }
        else if(GotoScanConfigFlag)
        {
            ISCNIRScanSDK.GetActiveConfig();
        }
    }
    /*
     * When the activity is destroyed, unregister all broadcast receivers, remove handler callbacks,
     * and store all user preferences
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(NotifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefCoeffDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(CalMatrixDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(SpectrumCalCoefficientsReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnCurrentScanConfigurationDataReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampRampUpADCReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampADCAverageReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnMFGNumReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunSetLampReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunSetPGAReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunSetScanRepeatsReceiver);
        mHandler.removeCallbacksAndMessages(null);
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.continuousScan, toggle_btn_continuous_scan.isChecked());
    }
    @Override
    public void onPause() {
        super.onPause();
        //back to desktop,should disconnect to device
        if(!GotoOtherPage)
            finish();
    }
    private class  BackGroundReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
           finish();
        }
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link HomeViewActivity}
     * and display a toast message
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
    /**
     *Notify ble service to know the device is extension or not
     */
    private void Notify_IsEXTVersion()
    {
        Intent notify_IsextVer = new Intent(NOTIFY_ISEXTVER);
        notify_IsextVer.putExtra("ISEXTVER",isExtendVer);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(notify_IsextVer);
    }
    private void UI_ShowMaxPattern()
    {
        int start_nm = Integer.parseInt(et_quickset_spec_start.getText().toString());
        int end_nm =  Integer.parseInt(et_quickset_spec_end.getText().toString());
        int width_index = quickset_scan_width_index;
        int num_repeat = Integer.parseInt(et_quickset_average_scan.getText().toString());
        int scan_type = quickset_scan_method_index;
        int IsEXTver = 0;
        if(isExtendVer)
        {
            IsEXTver = 1;
        }
        MaxPattern = GetMaxPattern(start_nm,end_nm,width_index,num_repeat,scan_type,IsEXTver);
        String text = "D-Res. (pts, max:" + MaxPattern +")";
        tv_quickset_res.setText(text);
    }
    /**
     *  Set device physical button status
     */
    private void SetDeviceButtonStatus()
    {
        if(fw_level.compareTo(FW_LEVEL.LEVEL_1)>0)
        {
            Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,true);
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
    /**
     *Get max pattern that user can set
     */
    private int GetMaxPattern(int start_nm,int end_nm,int width_index,int num_repeat,int scan_type,int IsEXTver)
    {
        return ISCNIRScanSDK.GetMaxPatternJNI(scan_type,start_nm,end_nm,width_index,num_repeat,SpectrumCalCoefficients,IsEXTver);
    }
    /**
     * Change scan config to byte array
     */
    public byte[] ChangeScanConfigToByte()
    {
        ISCNIRScanSDK.ScanConfigInfo write_scan_config = new ISCNIRScanSDK.ScanConfigInfo();
        //transfer config name to byte
        String isoString ="QuickSet";
        int name_size = isoString.length();
        byte[] ConfigNamebytes=isoString.getBytes();
        for(int i=0;i<name_size;i++)
        {
            write_scan_config.configName[i] = ConfigNamebytes[i];
        }
        write_scan_config.write_scanType = 2;
        //transfer SerialNumber to byte
        String SerialNumber = "12345678";
        byte[] SerialNumberbytes=SerialNumber.getBytes();
        int SerialNumber_size = SerialNumber.length();
        for(int i=0;i<SerialNumber_size;i++)
        {
            write_scan_config.scanConfigSerialNumber[i] = SerialNumberbytes[i];
        }
        write_scan_config.write_scanConfigIndex = 255;
        write_scan_config.write_numSections =(byte) 1;
        write_scan_config.write_numRepeat = Integer.parseInt(et_quickset_average_scan.getText().toString());
        numSections=1;

        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionScanType[i] = (byte)spin_quickset_scan_method.getSelectedItemPosition();
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWavelengthStartNm[i] =Integer.parseInt(et_quickset_spec_start.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWavelengthEndNm[i] =Integer.parseInt(et_quickset_spec_end.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionNumPatterns[i] =Integer.parseInt(et_quickset_res.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWidthPx[i] = (byte)(spin_quickset_scan_width.getSelectedItemPosition()+2);
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionExposureTime[i] = spin_quickset_exposure_time.getSelectedItemPosition();
        }
        return ISCNIRScanSDK.WriteScanConfiguration(write_scan_config);
    }
    /**
     * Compare the device's  config  and user settings are the same or not
     * @param EXTRA_DATA  scan config byte data
     */
    public Boolean Compareconfig(byte EXTRA_DATA[])
    {
        if(EXTRA_DATA.length!=155)
        {
            return false;
        }
        ISCNIRScanSDK.ScanConfiguration config = ISCNIRScanSDK.current_scanConf;
        if(reference_set_config)//check reference setting
        {
            reference_set_config = false;
            if(config.getSectionScanType()[0]!= (byte)0)
            {
                return false;
            }
            if(Integer.parseInt(et_quickset_spec_start.getText().toString())!=MINWAV)
            {
                return false;
            }
            if(Integer.parseInt(et_quickset_spec_end.getText().toString())!=MAXWAV)
            {
                return false;
            }
            if(config.getSectionWidthPx()[0]!=(byte)6)
            {
                return false;
            }
            if(config.getSectionNumPatterns()[0]!=228)
            {
                return false;
            }
            if(config.getSectionNumRepeats()[0]!=6)
            {
                return false;
            }
            if( config.getSectionExposureTime()[0]!=0)
            {
                return false;
            }
        }
        else //check quickset setting
        {
            if(config.getSectionScanType()[0]!= spin_quickset_scan_method.getSelectedItemPosition())
            {
                return false;
            }
            if(config.getSectionWavelengthStartNm()[0]!=Integer.parseInt(et_quickset_spec_start.getText().toString()))
            {
                return false;
            }
            if(config.getSectionWavelengthEndNm()[0]!=Integer.parseInt(et_quickset_spec_end.getText().toString()))
            {
                return false;
            }
            if(spin_quickset_scan_width.getSelectedItemPosition()+2 != config.getSectionWidthPx()[0])
            {
                return false;
            }
            if(config.getSectionNumPatterns()[0]!=Integer.parseInt(et_quickset_res.getText().toString()))
            {
                return false;
            }
            if(config.getSectionNumRepeats()[0]!=Integer.parseInt(et_quickset_average_scan.getText().toString()))
            {
                return false;
            }
            if( config.getSectionExposureTime()[0]!=spin_quickset_exposure_time.getSelectedItemPosition())
            {
                return false;
            }
        }
        return true;
    }
    public void SetReferenceParameter()
    {
        reference_set_config = true;
        ISCNIRScanSDK.SetReferenceParameter(MINWAV,MAXWAV);
    }
    /**
     *Perform scan to the device
     * @param delaytime  set delay time tto avoid ble hang
     */
    private void PerformScan(long delaytime)
    {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                //Send broadcast START_SCAN will trigger to scan data
                ISCNIRScanSDK.StartScan();
            }}, delaytime);
    }
    //region Receiver
    /**
     *  Success set Lamp state(ISCNIRScanSDK.LampState should be called)
     */
    public class RetrunSetLampReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set lamp on,off,auto
        }
    }
    /**
     *  Success set PGA( ISCNIRScanSDK.SetPGA should be called)
     */
    public class RetrunSetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set pga
        }
    }
    /**
     *  Success set Scan Repeats( ISCNIRScanSDK.setScanAverage should be called)
     */
    public class RetrunSetScanRepeatsReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set scan repeats
        }
    }

    //  显示报告页面
    private void showDialog(String arr){
        JSONArray ja = JSONArray.parseArray(arr);
        String v = "";
        for(int i =0; i<ja.size();i++) {
            v += ja.getJSONObject(i).get("name")+"："+ja.getJSONObject(i).get("value")+" \n";
        }

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.led_g);
        builder.setTitle("预测");
        builder.setMessage(v);
        builder.setPositiveButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog=builder.create();
        dialog.show();
    }
    //endregion
}
