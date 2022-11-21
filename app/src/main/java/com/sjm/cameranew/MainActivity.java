package com.sjm.cameranew;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private boolean switchPic = false;
    static int idx = 0;
    private String ipAddress = "";
    private String ipList[] = null;
    private List<String> iparraylist = new ArrayList<String>();
    private boolean bLoop = true;
    private String m_tolerance = "";
    private String m_eMail = "";
    private String m_eMailPW = "";
    private String m_eMailNum = "";
    private Boolean m_sendMail = true;
    private Boolean m_bPreview = false;
    private Boolean m_alwaysnotify = false;
    private boolean m_itemSelected = false;
    //private Integer m_selectedRingtoneIndex = 0;
    private String m_sInterval = "";
    private String m_sRingTone = "1";
    private Intent m_serviceIntent;
    private List m_chkList = null;
    private String m_sIP = "";
    private String m_targetViewerIP="";
    public Activity m_activity;
    public  int PORT_MSG = 6667;
    public  int PORT_MSG_CONNECTED = 6669;
    //private static int PORT_KILL = 9997;
    //private  int PORT_MSG = 6667;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        m_activity = this;
        //We need full access to the filesystem.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if(!Environment.isExternalStorageManager()) {
//                try {
//                    Uri uri = Uri.parse("package:" +  BuildConfig.APPLICATION_ID);
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
//                    startActivity(intent);
//                } catch (Exception ex) {
//                    Intent intent = new Intent();
//                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                    startActivity(intent);
//                }
//            }
//        }
        Runnable r = new initialLoad();
        new Thread(r).start();
        View view = this.getWindow().getDecorView();
        setTitle("PRO Motion Detector Camera");
        view.setBackgroundResource(R.color.back_color);

        Runnable rThreadReceiveMulticast = new recMulticast();
        new Thread(rThreadReceiveMulticast).start();

        //init preferences
        PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences sharedPref =
                PreferenceManager
                        .getDefaultSharedPreferences(this);

        m_eMail = sharedPref.getString("key_email_address", "");
        m_eMailPW = sharedPref.getString("key_email_PW", "");
        m_eMailNum = sharedPref.getString("key_email_number", "");
        m_tolerance = sharedPref.getString("key_sensitivity", "4.4"); // .getString("key_sensitivity");
        m_bPreview = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_PREVIEW, false);
        m_sInterval = sharedPref.getString(getString(R.string.key_interval), "1");
        m_sendMail = sharedPref.getBoolean("key_checkbox_email", true);
        m_alwaysnotify = sharedPref.getBoolean("key_checkbox_alwaysnotify", false);
        Boolean bSoundAlarm = sharedPref.getBoolean("key_checkbox_alarm", true);
        if (bSoundAlarm) {
            m_sRingTone = sharedPref.getString("key_alarm", ""); //to be eventually passed to the Monitor app.
        } else {
            m_sRingTone = "-1";
        }
        final Button btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                switchPic = !switchPic;
                try {
                    final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                    btnConnect.startAnimation(animBounce);
//                    m_targetViewerIP = "";
//                    m_itemSelected = false;
                    RefreshList();
                    handlePermissions();
                } catch (Exception e) {
                    //Toast.makeText(getApplicationContext(), "btnConnect_Click Err(0) : " + e, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        final Button btnSnapshot = findViewById(R.id.btnSnapshot);
        btnSnapshot.setEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handlePermissions();

        Runnable r1 = new ThreadServer(this,PORT_MSG);   //Send Data
        new Thread(r1).start();
        //Toast.makeText(getApplicationContext(), "Please ensure the PRO Motion Detector Viewer app is running." , Toast.LENGTH_SHORT).show();

        ImageView image = (ImageView) findViewById(R.id.imgCloud);
        ListView lv = (ListView) findViewById(R.id.lvSources);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cloud3);
        image.setImageBitmap(bitmap);



//        image.setVisibility(View.VISIBLE);
//        lv.setVisibility(View.GONE);

//        Bitmap bm = BitmapFactory.decodeResource(getResources(),R.drawable.cloud3);
//        LinearLayout linearLayout= new LinearLayout(this);
//        linearLayout.setOrientation(LinearLayout.VERTICAL);
//        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                ActionBar.LayoutParams.MATCH_PARENT));
//        ImageView imageView = new ImageView(this);
//        speechBubble(bm);
//        //imageView.setImageBitmap(bm);
//        imageView.setImageResource(R.drawable.cloud3);
//        imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT));
//        linearLayout.addView(imageView);
//        setContentView(linearLayout);
    }
    public static Bitmap speechBubble(Bitmap bm) {
        final Bitmap output = Bitmap.createBitmap(bm.getWidth(),bm.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        //final Canvas canvas = new Canvas(bm);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        //canvas.drawBitmap(bm,0,0,paint);
        canvas.drawBitmap(output,0,0,paint);

        return output;
    }

    public void handlePermissions() {
    askPermission();
}
    public void askPermission()
    {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}; // List of permissions required

        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, PERMISSION_ALL);
                return;
            }
        }
        final Button btnSnapshot = findViewById(R.id.btnSnapshot);
        btnSnapshot.setEnabled(true);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final Button btnSnapshot = findViewById(R.id.btnSnapshot);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    btnSnapshot.setEnabled(true);
                } else {
                    Toast.makeText(this, "Until you grant the permission, we cannot proceed further. Tap the RE-SCAN button to try again.", Toast.LENGTH_SHORT).show();
                    btnSnapshot.setEnabled(false);
                }
                return;
            }
        }
    }
    public String GetDeviceipWiFiData() {
        @SuppressWarnings("deprecation")
        android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE);
        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.layout_landscape);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main);
        }
        //***********************************************************************************
        //WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //int currentRotation = windowService.getDefaultDisplay().getRotation();
        //int i = 0;
        //***********************************************************************************
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, com.sjm.cameranew.SettingsActivity.class);
            startActivity(intent);
            return true;

        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    public class initialLoad implements Runnable {
        public initialLoad() {
            // store parameter for later user
        }

        public void run() {
            arrayAdapterListView(false);
        }
    }
    public class recBroadcast implements Runnable {
        public recBroadcast() {
            // store parameter for later user
        }

        public void run() {
            receiveBroadcast recB = new receiveBroadcast(); //listen for Viewer app UDP broadcast
            m_targetViewerIP =  recB.getsViewerIP();
        }
    }
    public class recMulticast implements Runnable {
        public recMulticast() {
            // store parameter for later user
        }

        public void run() {
            MulticastReceiver mc = new MulticastReceiver(); //listen for Viewer app UDP broadcast
            //m_targetViewerIP =  recB.getsViewerIP();
            m_targetViewerIP =  mc.getsViewerIP();
        }
    }
//    @Override
//    protected void  onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        Rect ourRect = new Rect();
//        ourRect.set(0,0,canvas.getWidth(),canvas.getHeight()/2);
//        canvas.drawRect(ourRect,myPaint);
//    }
    @Override
    protected void onStop() {

        Log.d("SJMDEBUG", "Attempting to stop Activity(0)");

        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SJMDEBUG", "Attempting to stop Activity(1)");

    }
    @Override
    protected void onResume() {
        try {
            super.onResume();
        } catch (Exception e) {
            Log.e("SJMOnResume Error", "Error: " + e);
            e.printStackTrace();
        }
    }

    //public  class Helper {

        public  boolean isAppRunning(final Context context, final String packageName) {
//            ActivityManager actvityManager = (ActivityManager)
//                    this.getSystemService( ACTIVITY_SERVICE );
//            List<ActivityManager.RunningAppProcessInfo> procInfos = actvityManager.getRunningAppProcesses();
//
//            for(ActivityManager.RunningAppProcessInfo runningProInfo:procInfos){
//
//                Log.d("Running Processes", "()()"+runningProInfo.processName);
//            }
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
            if (procInfos != null)
            {
                for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                    Log.d("Executed app", "Application executed : " + processInfo.processName);
                    if (processInfo.processName.equals(packageName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    //}

    // This method uses an ArrayAdapter to add data in ListView.
    private void arrayAdapterListView(boolean auto) {
        final ListView listView = (ListView) findViewById(R.id.lvSources);
        final boolean autoload = auto;
        m_chkList = new ArrayList<CheckBox>();

        TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
        editTextAddress.setBackgroundColor(Color.BLACK);
        editTextAddress.setTextColor(Color.YELLOW);
        try {
            //Log.d("[DEBUG]", "LOADING");
            List<String> dataList = new ArrayList<String>();
            //ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, dataList) {
            ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.listview_row, R.id.labelIP, dataList) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = null;
                    try {
                        view = super.getView(position, convertView, parent);
                        view.setSelected(true);
                        TextView textViewIP = view.findViewById(R.id.labelIP);
                        TextView textViewIPDesc = view.findViewById(R.id.labelIPDesc);

                        ViewGroup.LayoutParams params = view.getLayoutParams();

                        if (position >= iparraylist.size())
                            return view;
                        //if (autoload)
                        //Log.d("[DEBUG]", "AUTO LOADING....... " + position);
                        m_chkList.add((CheckBox) view.findViewById(R.id.list_view_item_checkbox));
                        String[] arrOfIP = iparraylist.get(position).split(" ", 2); //pull out IP from string
                        String partIP, partIPDesc;
                        partIP = arrOfIP[0];
                        m_sIP = partIP;
                        String ipDeviceName = arrOfIP[1];
                        //listView.invalidate();
                        if (m_targetViewerIP.equals(m_sIP) && !m_targetViewerIP.equals(null)){
                            Log.d("[DEBUG]", "Found Viewer Client IP: " + m_targetViewerIP);
                            CheckBox chk = view.findViewById(R.id.list_view_item_checkbox);
                            chk.setChecked(true);
                            m_itemSelected = true;
                            TextView editTextAddress = findViewById(R.id.editTextAddress);
                            ipAddress = m_targetViewerIP;
                            editTextAddress.setText(ipDeviceName);
                            editTextAddress.invalidate();

                            final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                            final TextView text = findViewById(R.id.editTextAddress);

                            text.startAnimation(animBounce);
                            m_itemSelected = true;
                            Bubble bubble = new Bubble();
                            bubble.showBubble(m_activity,"Connected to Viewer OK.\nClick the red X\nThen press Start");
                        }
                        /*** GET DEVICE NAME (not used ( yet ) )*****************************************************/
                        /**********************************************************/
                        partIPDesc = arrOfIP[1];
                        textViewIP.setTextColor(Color.RED);
                        textViewIP.setTextSize(12);
                        textViewIP.setText("   " + partIP + "\t\t");
                        textViewIPDesc.setTextColor(Color.GREEN);
                        textViewIPDesc.setTextSize(12);
                        textViewIPDesc.setText(partIPDesc);
                        if (!m_itemSelected){
                            TextView editTextAddress = findViewById(R.id.editTextAddress);
                            //editTextAddress.setText("Select Viewing device from the list.");
                            //editTextAddress.setText("This device will use the Camera, now select a Viewing device from the list.");
                            editTextAddress.setText("This device will use the Camera, is the Viewer app running? ");
                            Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                            editTextAddress.startAnimation(animBounce);

                        }

                        // Set the height of the Item View
                        try {
                            if (!params.equals(null)) {
                                int screenHeight = ((WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
                                params.height = screenHeight / 33;
                                //params.height  = 95;
                                view.setLayoutParams(params);

                                //LinearLayout.LayoutParams vi_params =
                                //        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, (int)(screenHeight*0.33));
                                //view.setLayoutParams(vi_params);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //Log.d("[DEBUG]", "LOADING....... " + position);
                        return view;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return view;
                }
            };

            boolean bLoop = true;
            int numTrys = 15, counter = 0;

            //new MyIPThread(getApplicationContext());

            Thread tip = new MyThreadIP();
            tip.start();
            //tip.join();

            while (bLoop) {
                if (counter >= numTrys) {
                    break;
                }
                //Thread.sleep(100);
                Thread.sleep(100);
                counter++;
                //Log.d("[DEBUG]", "ITERATION: " + counter);

                if (iparraylist.size() > 0) {
                    for (int i = 0; i < iparraylist.size(); i++) {
                        dataList.add(iparraylist.get(i));
                    }

                    listView.setAdapter(listAdapter);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                TextView editTextAddress = findViewById(R.id.editTextAddress);
                //clear checkboxes
                for (int i = 0; i < m_chkList.size(); i++) {
                    CheckBox chkUncheck = (CheckBox) m_chkList.get(i);
                    chkUncheck.setChecked(false);
                }
                //set selected checkbox
                CheckBox chk = (CheckBox) m_chkList.get(position);
                chk.setChecked(true);

                Object clickItemObj = adapter.getAdapter().getItem(position);
                ipAddress = clickItemObj.toString();
                String[] arrOfStr = ipAddress.split(" ", 2); //pull out IP from string
                ipAddress = arrOfStr[0];
                //String ipDeviceName = arrOfIP[1];
                editTextAddress.setText(arrOfStr[1]);
                final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                final TextView text = findViewById(R.id.editTextAddress);
                text.startAnimation(animBounce);
                //Toast.makeText(getApplicationContext(), "Please keep device still..." + "\n" + "\n" + "Press 'Start' and then start the PRO Motion Viewer app if not running already.", Toast.LENGTH_SHORT).show();
                Bubble bubble = new Bubble();
                bubble.showBubble(m_activity,"Press Start button...\nViewer app should be running.\nIf not start it up.");
            }
        }
        );

    }

    class MyThreadIP extends Thread {
        ProgressBar pb;
        String sIP;
        public void run() {
            // Get base IP (nnn.nnn.n) from any connected network
            try {
                String sIPAlt = GetDeviceipWiFiData();
                String[] arrOfIP = sIPAlt.split("\\."); //pull out IP from string
                String s4 = arrOfIP[0] + "." + arrOfIP[1] + "." + arrOfIP[2];
                sIP = s4;
                if (!s4.equals("0.0.0")) {
                    // pass base ip to scanner
                    com.sjm.cameranew.ScanNet snet = new com.sjm.cameranew.ScanNet();
                    pb = findViewById(R.id.progressBar);
                    TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);

                    if (!m_itemSelected) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextAddress.setText("Scanning local network...");
                            }
                        });
                    }
//                    else {
//                        editTextAddress.setText(m_targetViewerIP);
//                    }
                    //************************************************************
                    List<String> iplist = new ArrayList<String>();
                    iplist = snet.main(s4, getApplicationContext()); //pull IP submask

                    // iterate the list & only add whats not already there.
                    for (int i = 0; i < iplist.size(); i++) {
                        if (!iparraylist.contains(iplist.get(i).toString())) {
                            iparraylist.add(iplist.get(i));
                        }
//                        if (m_targetViewerIP.equals(iplist.get(i))){
//                            Log.d("[DEBUG IP SCAN]", "MATCH");
//                        }
                    }
                    Log.d("[DEBUG IP SCAN]", "Got Results");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (iparraylist.isEmpty()) {
                return;
            }
            //
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean itemchecked = false;
                    // Stuff that updates the UI
                    View v = findViewById(R.id.progressBar);
                    pb.setVisibility(v.GONE);
                    for (int i = 0; i < m_chkList.size(); i++) {
                        CheckBox chkUncheck = (CheckBox) m_chkList.get(i);
                        if (chkUncheck.isChecked()) {
                            itemchecked = true;
                            //try to set listview item into view
                            final ListView listView = (ListView) findViewById(R.id.lvSources);
                            listView.smoothScrollToPosition(i);
                            try {
                                listView.getAdapter().getView(0, null, null).performClick();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }


                    if (!itemchecked) { //if an item has been selected don't refresh the ip list
                        RefreshList(); //auto refresh IP list once it's been loaded into iparraylist
                    }
                    if (!sIP.equals("0.0.0") && (!itemchecked) ) {
                        TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                        editTextAddress.setText("Scanning : " + sIP + ".x" + "\n" + "\n" + "Searching for Viewer app.");
                    }
                    else {
                        if (!itemchecked) {
                            TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                            editTextAddress.setText("Check you're connected to the network!");
                        }
                    }
                }
            });
        }
    }

    private String getSubnetAddress(int address) {
        String ipString = String.format(
                "%d.%d.%d",
                (address & 0xff),
                (address >> 8 & 0xff),
                (address >> 16 & 0xff));

        return ipString;
    }

    public void RefreshList() {
        switchPic = !switchPic;
        try {
            //MyThread t = new MyThread();
            //t.start();
            arrayAdapterListView(false);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Err(0) : " + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    public void RefreshList(View view) {

    }
    public void takeSnapshot(View view) {
        idx = 0; //init
        final Button btnSnapshot = findViewById(R.id.btnSnapshot);
        final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
        btnSnapshot.startAnimation(animBounce);
        takePic();
        if (ipAddress == "") {
            return;
        }

        btnSnapshot.setText("Transmitting...");

    }

    public void StopService(View view) {
        //Log.d("[DEBUG]", "STOPPING SERVICE");
        stopService(m_serviceIntent);
        finish();
    }

    private void takePic() {
        startService();
        //launchActivity(); //this is the older version where we call CameraActivity which in turn starts Camera2Service,now we bypass CameraActivity

    }

    public void startService() {
        //========================================================================
        if (ipAddress == "") {
            //Toast.makeText(getApplicationContext(), "Target IP not selected... ", Toast.LENGTH_SHORT).show();
            Bubble bubble = new Bubble();
            bubble.showBubble(m_activity,"Target IP not selected... ");
            return;
        }
        Thread t = new Thread() {
            public void run() {
                m_serviceIntent = new Intent(MainActivity.this, com.sjm.cameranew.Camera2Service.class);
                m_serviceIntent.putExtra("ipaddress", ipAddress);
                //m_serviceIntent.putExtra("orientation", mOrientation);
                m_serviceIntent.putExtra("tolerance", m_tolerance);
                m_serviceIntent.putExtra("eMail", m_eMail);
                m_serviceIntent.putExtra("eMailPW", m_eMailPW);
                m_serviceIntent.putExtra("eMailNum", m_eMailNum);
                m_serviceIntent.putExtra("sendMail", m_sendMail);
                m_serviceIntent.putExtra("ringtone", m_sRingTone);
                m_serviceIntent.putExtra("allwaysNotify", m_alwaysnotify);

                //Log.d("[DEBUG] ", "CameraActivity EMAIL: " + m_eMail);

                ContextCompat.startForegroundService(MainActivity.this, m_serviceIntent);
                //ContextCompat.startservice startForegroundService(MainActivity.this, m_serviceIntent);

            }
        };
        t.start();
        int oi = 0;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String strStatus = data.getStringExtra("Status");
                //Log.d("[DEBUG]", "[BACK FROM INTENT]==>" + strStatus);
                TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                if (strStatus.equals("Failed"))
                    editTextAddress.setTextColor(Color.RED);
                editTextAddress.setText("Could not contact Viewer - is it running?");
            }
        }
    }

    class ThreadClient  implements Runnable {
        private String ipCallingAddress;
        private int PORT_KILL_ACTIVITY = 6667;
        private int PORT_KILL_SERVICE = 6668;
        private int port;
        public ThreadClient(String ip, String msg,int p) {
            //sMsg = msg;
            ipCallingAddress = ip;
            port = p;
        }

        @Override

        public void run() {
            try {
                GreetClient client = new GreetClient();
                client.startConnection(ipCallingAddress, port);
                String response = client.sendMessage("hello server");
                Log.d("[DEBUG]", "REPLY FROM SERVER " + response + " : " +  ipCallingAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void end(){
        this.finish();
    }

}
class ThreadServer  implements Runnable{
    private Activity act ;
    private com.sjm.cameranew.Camera2Service cam;
    private int PORT_MSG;
    @SuppressWarnings("deprecation")
    public ThreadServer(Activity a,int PORT )
    {
        act = a;
        PORT_MSG = PORT;
    }
    public void run() {
        try {

            com.sjm.cameranew.GreetServer srv = new com.sjm.cameranew.GreetServer();
            String resp = srv.start(PORT_MSG);
            Log.d("SJMDEBUG", "response KILL SIGNAL: " + resp);
            //killService();
            if (PORT_MSG != 6669) { //test
                //if (resp.trim() != "") {
                //act.finishAndRemoveTask();
                //act.finishAffinity();
                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);
                act.finish();
            //}
            }
            else {
                Log.d("SJMDEBUG", "response CONNECT: " + resp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

