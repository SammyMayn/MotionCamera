package com.sjm.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
    //private Integer m_selectedRingtoneIndex = 0;
    private String m_sInterval = "";
    private String m_sRingTone = "";
    private Intent m_serviceIntent;
    private List m_chkList = null;
    private String m_sIP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Runnable r = new initialLoad();
        new Thread(r).start();
        View view = this.getWindow().getDecorView();
        setTitle("PRO Motion Detector Camera");
        view.setBackgroundResource(R.color.back_color);

        //init preferences
        androidx.preference.PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences sharedPref =
                androidx.preference.PreferenceManager
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
        WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int currentRotation = windowService.getDefaultDisplay().getRotation();
        int i = 0;
        //***********************************************************************************
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
    }

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
                        /*** GET DEVICE NAME (not used ( yet ) )*****************************************************/
//                        new Thread(new Runnable() {   // new thread for parallel execution
//                            public void run() {
//
//                                try {
//                                    /***************************************************************/
//                                    try {
//                                        InetAddress address = InetAddress.getByName (m_sIP);
//                                        String hostname = address.getCanonicalHostName ();
//                                        Log.d("[DEBUG IP SCAN]", "Hostname: " + hostname);
//                                    } catch (UnknownHostException e) {
//                                        e.printStackTrace();
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }).start();     // dont forget to start the thread
                        /**********************************************************/
                        partIPDesc = arrOfIP[1];
                        textViewIP.setTextColor(Color.RED);
                        textViewIP.setTextSize(12);
                        textViewIP.setText("   " + partIP + "\t\t");
                        textViewIPDesc.setTextColor(Color.GREEN);
                        textViewIPDesc.setTextSize(12);
                        textViewIPDesc.setText(partIPDesc);

                        TextView editTextAddress = findViewById(R.id.editTextAddress);
                        //editTextAddress.setText("Select Viewing device from the list.");
                        editTextAddress.setText("This device will use the Camera, now select a Viewing device from the list.");
                        Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                        editTextAddress.startAnimation(animBounce);

                        // Set the height of the Item View
                        params.height = 95;
                        view.setLayoutParams(params);
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
                                                editTextAddress.setText(arrOfStr[0]);
                                                final Animation animBounce = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);
                                                final TextView text = findViewById(R.id.editTextAddress);
                                                text.startAnimation(animBounce);
                                                Toast.makeText(getApplicationContext(), "Please keep device still..." + "\n" + "\n" + "Press 'Start' and then start the PRO Motion Viewer app", Toast.LENGTH_SHORT).show();
                                            }
                                        }
        );

    }


    class MyThreadIP extends Thread {
        ProgressBar pb;

        // String sIPArray[] ;
        public void run() {
            // Get base IP (nnn.nnn.n) from any connected network
            //String ipNet = startPingService( getApplicationContext());
            //if (ipNet == null) {
            //    return;
            //}
            try {

                //String sIP = getWifiIPAddress();
                String sIPAlt = GetDeviceipWiFiData();
                //String[] parts = sIP.split(".");
                //String s1 = parts[0];
                //String s2 = parts[1];
                String[] arrOfIP = sIPAlt.split("\\."); //pull out IP from string
                String s4 = arrOfIP[0] + "." + arrOfIP[1] + "." + arrOfIP[2];
                //sIPArray[] = sIP.split(".");
                if (!s4.equals("0.0.0")) {
                    // pass base ip to scanner
                    ScanNet snet = new ScanNet();
                    pb = findViewById(R.id.progressBar);
                    //populate IP list
                    //iparraylist = snet.main(ipNet);
                    TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                    editTextAddress.setText("Scanning : " + s4 + ".x");
                    iparraylist = snet.main(s4, getApplicationContext()); //pull IP submask

                    /****************************************************************/
                    //Log.d("[DEBUG IP SCAN]", "Got Results");
                } else {
                    TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                    editTextAddress.setText("Check you're connected to the network!");
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
                        }
                    }

                    if (!itemchecked) { //if an item has been selected don't refresh the ip list
                        RefreshList(); //auto refresh IP list once it's been loaded into iparraylist
                    }
                }
            });
        }
    }

    //Not used but here for future reference
    public String startPingService(Context context) {
        //List<LocalDeviceInfo> deviceInfoList  = new ArrayList<LocalDeviceInfo>();
        try {

            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            String subnet = getSubnetAddress(mWifiManager.getDhcpInfo().gateway);
            return subnet;
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
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

    /*
        private class AsyncTaskRunner extends AsyncTask<String, String, String> {

            private String resp = "";

            @Override
            protected String doInBackground(String... params) {
                publishProgress(params[0]); // Calls onProgressUpdate()
                return resp;
            }

            protected void onProgressUpdate(String... progress) {
                Toast.makeText(getApplicationContext(), progress[0], Toast.LENGTH_SHORT).show();
            }

            protected void onPostExecute(Long result) {

            }
        }
    */
/*
    private class AsyncContactServer extends AsyncTask<String, String, String> {

        private String resp = "";
        private Socket socket;
        private BufferedReader input;
        private String addr = "";

        @Override
        protected String doInBackground(String... params) {
            publishProgress(params[0]); // Calls onProgressUpdate()
            return resp;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(getApplicationContext(), progress[0], Toast.LENGTH_SHORT).show();
            TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
            addr = editTextAddress.getText().toString();

            //Log.d("[DEBUG]", "Got address: " + addr);
            try {
                Toast.makeText(getApplicationContext(), "[DEBUG GOT SOCKET]", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "[DEBUG ERR]" + e, Toast.LENGTH_SHORT).show();
            }
        }

        protected void onPostExecute(Long result) {

        }
    }
*/
    private void takePic() {
        startService();
        //launchActivity(); //this is the older version where we call CameraActivity which in turn starts Camera2Service,now we bypass CameraActivity

    }

    public void startService() {
        //========================================================================
        if (ipAddress == "") {
            Toast.makeText(getApplicationContext(), "Target IP not selected... ", Toast.LENGTH_SHORT).show();
            return;
        }
        Thread t = new Thread() {
            public void run() {
                m_serviceIntent = new Intent(MainActivity.this, Camera2Service.class);
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
    }

    /*
    private void launchActivity() {

        Intent intent = new Intent(MainActivity.this, CameraActivity.class);

        if (ipAddress == "") {
            Toast.makeText(getApplicationContext(), "Target IP not selected... ", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra("ipaddress", ipAddress);
        int i = Integer.parseInt(m_sInterval);
        intent.putExtra("runinterval", i);
        intent.putExtra("CamPreview", m_bPreview);
        intent.putExtra("tolerance", m_tolerance);
        intent.putExtra("eMail",m_eMail);
        intent.putExtra("eMailPW",m_eMailPW);
        intent.putExtra("eMailNum",m_eMailNum);
        intent.putExtra("sendMail",m_sendMail);
        intent.putExtra("ringtone",m_sRingTone);
        //startActivity(intent);
        startActivityForResult(intent, 1);

    }
*/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String strStatus = data.getStringExtra("Status");
                //Log.d("[DEBUG]", "[BACK FROM INTENT]==>" + strStatus);
                TextView editTextAddress = (TextView) findViewById(R.id.editTextAddress);
                if (strStatus.equals("Failed"))
                    editTextAddress.setTextColor(Color.RED);
                editTextAddress.setText("Could not contact Server - is it running?");
            }
        }
    }
}