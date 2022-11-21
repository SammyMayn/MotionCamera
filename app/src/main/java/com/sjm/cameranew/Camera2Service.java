package com.sjm.cameranew;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class Camera2Service extends Service {
    protected static final int CAMERA_CALIBRATION_DELAY = 500;
    protected static final String TAG = "myLog";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected String m_thisipaddress = "";
    protected String m_ipaddress = "";
    protected String m_filename = "";
    protected String m_Orientation = "PORTRAIT";
    protected String m_Tolerance = "15.5"; //default
    protected String m_eMail = "";
    protected String m_eMailPW = "";
    protected String m_eMailNum = "7";
    protected String m_ringtone = "";
    protected Boolean m_allwaysNotify = false;
    protected Boolean m_sendMail = true;
    protected CaptureRequest.Builder builder = null;
    protected int m_cnt = 0;
    protected int m_MillisecFramePeriod = 5;
    private Handler handler;
    private  int imgcnt = 0;
    private boolean m_receivedKillSignal;
    private boolean looper = true;
    private int PHOTOWIDTH = 220;//320;
    private int PHOTOHEIGHT = 220; //320;
    private Image m_currentImage;
    private ByteBuffer  streamBuff;
    private byte[]  streamBytes;
    private static String STOP_SERVICE = "com.sjm.cameranew";
    private static int PORT_IMAGES = 9999;
    private static int PORT_KILL = 9995;
    private static int PORT_MSG = 9997;
    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //Log.d(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }

    };
    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SJMDEBUG", "Camera Service onReceive " );

            if(intent.getAction().equals(STOP_SERVICE)){
                Log.d("SJMDEBUG", "Camera Service onReceive StopSelf" );
                Camera2Service.this.stopSelf();
            }
        }
    };
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Log.d("[DEBUG] Camera2Service OrientationMyApp", "Current Orientation : Landscape");
            m_Orientation = "LANDSCAPE";
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Log.d("[DEBUG]  Camera2Service OrientationMyApp", "Current Orientation : Portrait");
            m_Orientation = "PORTRAIT";
        }
        //***********************************************************************************
        //WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //int currentRotation = windowService.getDefaultDisplay().getRotation();
        //int i = 0;
        //***********************************************************************************
    }

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            //Log.e("[DEBUG]", "OnReady " );
            //String msg = "1";
            Camera2Service.this.session = session;
            CaptureRequest cr = createCaptureRequest();
            m_cnt++;

            try {
                //msg = "2";
                // if (m_cnt <=1) {
                if (!(cr == null)) {
                    session.setRepeatingRequest(cr, null, null);
                    //session.setRepeatingRequest(createCaptureRequest(), null, null);
                    //msg = "3";
                    cameraCaptureStartTime = System.currentTimeMillis();
                }
                //}
            } catch (CameraAccessException e) {
                Log.e("[DEBUG]", "CameraCaptureSession(0) : "  + e.getMessage());
                Camera2Service.this.stopSelf();
                Log.d("[DEBUG]", "Stooped Service " );
            }
            catch (Exception e) {
                Log.e("[DEBUG]", "CameraCaptureSession(1) : "  + e.getMessage());
                Camera2Service.this.stopSelf();
                Log.d("[DEBUG]", "Stooped Service " );
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Log.d(TAG, "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                    try {
                        Thread.sleep(m_MillisecFramePeriod);
                        //processImage(img);
                        byte[] bytesBuff = processImageNew(img); 
                        Runnable r = new sendImageNew(bytesBuff);   //Send Image
                        new Thread(r).start();
                        Runnable r1 = new sendDataNew();   //Send Data
                        new Thread(r1).start();

                        //sendImage();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                img.close();
            }
        }
    };
    /********************************************************************************************************************/
    /********************************************************************************************************************/
    class Task implements Runnable {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d("[SJM DEBUG]", "TASK class thread is      >"+Thread.currentThread().getName());
                    CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

                    try {
                        String pickedCamera = getCamera(manager);

                        if (ActivityCompat.checkSelfPermission(Camera2Service.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        manager.openCamera(pickedCamera, cameraStateCallback, null);

                        imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
                        imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
                        //Log.d(TAG, "imageReader created");
                        //} catch (CameraAccessException e){
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }

    /********************************************************************************************************************/
    /********************************************************************************************************************/

    public String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cLens = characteristics.get(CameraCharacteristics.LENS_FACING);
                //int rot = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                int cOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                //int cOrientation = 270;
                //Log.e("[DEBUG]", "SENSOR ORIENTATION : " + cOrientation);
                if (cLens == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);
        if (intent != null) { //sjm added 07/04/2020 (april)
            broadcastStatus();
            String ip=GetMyIPAddress();
            m_thisipaddress = ip;
            m_ipaddress = intent.getStringExtra("ipaddress");
            m_Orientation = intent.getStringExtra("orientation");
            m_Tolerance = intent.getStringExtra("tolerance");
            m_eMail = intent.getStringExtra("eMail");
            m_eMailPW = intent.getStringExtra("eMailPW");
            m_eMailNum = intent.getStringExtra("eMailNum");
            m_sendMail = intent.getBooleanExtra("sendMail", true);
            m_ringtone = intent.getStringExtra("ringtone");
            m_allwaysNotify = intent.getBooleanExtra("allwaysNotify", false);
            //Log.d(TAG, "onStartCommand EMAIL " + m_eMail);

            /*********************************************************************/
            //m_filename = fPath + "dummy.jpg";
//            String fPath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
//            File myDir;
//            myDir = new File(fPath + "Motion/");
//            if (!myDir.isDirectory()) {
//                try {
//                    myDir.mkdirs();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            m_filename = fPath + "/Motion/dummy.jpg";
            //****************************************************************
            //need to ensure file exists first
//            File file = new File(m_filename);
//            if (!file.exists()){
//                try {
//                    FileWriter myWriter = null;
//                    try {
//                        myWriter = new FileWriter(m_filename);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    myWriter.write("Temp dummy file!");
//                    myWriter.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            //*******************************************

            new Thread(new Task()).start();

            //Thread trc = new readyCameraThread();
            //trc.start();
            //readyCamera();
        }
        //else
        //Log.d(TAG,"onCreate service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d("[DEBUG]","onCreate Camera2Service");
        super.onCreate();
        m_receivedKillSignal = false;
        /* ***** NOTIfICATION CHANNEL *********************************************** */
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "camera_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Camera Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
        /* ************************************************************************* */

        handler = new Handler();

    }

    private void broadcastStatus() {
        //Log.i("NLService", "Broadcasting status added("+nAdded+")/removed("+nRemoved+")");
        Intent i1 = new Intent("SERVICEMSG");
        i1.putExtra("serviceMessage", "Test from Service");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i1);
        // sendBroadcast(i1); //broadcast back to calling activity (not used but could be)

    }

    public void actOnReadyCameraDevice() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            //Log.e(TAG, "Camera2Service actOnReadyCameraDevice - CameraAccessException: " + e.getMessage());
        }
    }
    @Override
    public void onDestroy(){
        Log.d("SJMDEBUGSERVICE","killing Service(0)");
        super.onDestroy();
        Camera2Service.this.stopSelf();
        Log.d("SJMDEBUGSERVICE(0)","Service killed(1)");
    }

    public class sendImageNew implements Runnable {

        byte[] bytes;
        boolean success = false;
        DatagramSocket ds = null;
        int Angle = 0;
        InputStream input;
        byte[] bytesStreamBuff = new byte[0];
        public sendImageNew(byte[] bytesBuff ) {
            // store parameter for later use
            bytesStreamBuff = bytesBuff;
        }

        public void run() {

            try {
                if (m_receivedKillSignal) {
                    return;
                }
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    m_Orientation = "PORTRAIT";
                } else {
                    m_Orientation = "LANDSCAPE";
                }

                try {
                    input = new ByteArrayInputStream(bytesStreamBuff);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Bitmap bm = null;
                try {
                    //bm = BitmapFactory.decodeStream(fis);
                    bm = BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // rotate stream image 90 degrees (so server displays bitmap correctly)
                if (m_Orientation.trim().equals("PORTRAIT"))
                    Angle = 90;
                else
                    Angle = 0;

                Bitmap rotatedImg = null; // rotate(bm,filename);
                try {
                    rotatedImg = rotate(Angle, bm);
                    bytes = getBytesFromBitmap(rotatedImg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Prepare DatagramSocket
                ds = new DatagramSocket(null);
                DatagramPacket DpSend =
                        new DatagramPacket(bytes, bytes.length, InetAddress.getByName(m_ipaddress), PORT_IMAGES);
                ds.send(DpSend);
                //Log.d("SJMDEBUG", "Camera Service SENT IMAGE : " + imgcnt);
                imgcnt++;
                input.close();
                ds.close();

                if (imgcnt==22 ) {
                    Runnable r2 = new ThreadKillServer(6668);   //Send KILL signal
                    new Thread(r2).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class ThreadKillServer  implements Runnable{
        private Activity act ;
        private Camera2Service cam;
        private int PORT_MSG_KILL;
        @SuppressWarnings("deprecation")
        public ThreadKillServer(int PORT )
        {
            PORT_MSG_KILL = 6668;
        }
        public void run() {
            try {

                com.sjm.cameranew.GreetServer srv = new com.sjm.cameranew.GreetServer();
                String resp = srv.start(PORT_MSG_KILL);
                Log.d("SJMDEBUG", "response: " + resp);
                killService();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public class sendDataNew implements Runnable {
        private String sErr = "";
        private DatagramSocket ds;
        private byte[] databyte;

        public sendDataNew() {
            // store parameter for later use
        }

        public void run() {
            String sOrientation = "";

            try {
                ds = new DatagramSocket();
            } catch (IOException e) {
                sErr += "[00]" + e;
                e.printStackTrace();
                return;
            } catch (Exception e) {

                e.printStackTrace();
                //Log.d("[DEBUG]", "[ERROR(222)]]: " + e);
            }
            try {
                if (m_Orientation.trim().equals("PORTRAIT")) {
                    //Angle = 90;
                    sOrientation = "~PORTRAIT";
                } else {
                    //Angle = 0;
                    sOrientation = "~LANDSCAPE";
                }

                String outStr = m_Tolerance + sOrientation + "~" + m_eMail + "~" + m_eMailPW + "~" + m_eMailNum + "~" + m_sendMail + "~" + m_ringtone + "~"  + m_allwaysNotify + "~" + m_thisipaddress + "~terminator";
                //Log.d("[DEBUG]", "OUTPUTSTRING: " + outStr);
                databyte = outStr.getBytes();
                DatagramPacket DpSend =
                        new DatagramPacket(databyte, databyte.length, InetAddress.getByName(m_ipaddress), 8081);
                ds.send(DpSend);
                //Log.d("[DEBUG]", "[Data Thread Packet Sent : " + packetcnt++);
                ds.close();

            } catch (IOException e) {
                sErr += "[33]" + e;
                e.printStackTrace();
            } catch (Exception e) {
                sErr += "[44]" + "[DEBUG]" + e;
            }

        }

    }
    public static String getIpAddress()
    {
        URL myIP;
        try {

            myIP = new URL("https://myip.dnsomatic.com/");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(myIP.openStream())
            );
            return in.readLine();
        } catch (Exception e)
        {
            try
            {
                myIP = new URL("https://api.externalip.net/ip/");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(myIP.openStream())
                );
                return in.readLine();
            } catch (Exception e1)
            {
                try {
                    myIP = new URL("https://icanhazip.com/");

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(myIP.openStream())
                    );
                    return in.readLine();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        return null;
    }
    public String GetMyIPAddress() {

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    final String ip = addr.getHostAddress();
                    if(Inet4Address.class == addr.getClass()) return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return null;

    }

    private boolean killService() {
        @SuppressWarnings("deprecation")
        ActivityManager manager;
        manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            try {
                manager.killBackgroundProcesses("com.sjm.cameranew.Camera2Service");
                Intent serviceIntent = new Intent(this,Camera2Service.class);
                stopService( serviceIntent);
                Log.d("[DEBUGSJM]", "Stopping Service");
                m_receivedKillSignal = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("[DEBUGSJM]", "[ERROR StopService: " + e);
            }
            return true;
            //if (serviceClass.getName().equals(service.service.getClassName())) {
            //    return true;
            //}
        }

        //com.sjm.cameranew.Camera2Service
        return false;
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
//    private void processImage(Image image) {
//        //Process image data
//        ByteBuffer buffer;
//        byte[] bytes;
//        m_currentImage = image;
//        File file = new File(m_filename);
//        String fPath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
//        File myDir;
//        myDir = new File(fPath + "Motion/");
//        if (!myDir.isDirectory()) {
//            try {
//                myDir.mkdirs();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        //need to ensure file exists first
//        if (!file.exists()){
//            try {
//                FileWriter myWriter = null;
//                try {
//                    myWriter = new FileWriter(m_filename);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Error: Ensure App has Write Access to Storage", Toast.LENGTH_LONG).show();
//                    return;
//                }
//                myWriter.write("Temp dummy file!");
//                myWriter.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        FileOutputStream output = null;
//
//            if (image.getFormat() == ImageFormat.JPEG) {
//                buffer = image.getPlanes()[0].getBuffer();
//                bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
//                buffer.get(bytes); // copies image from buffer to byte array
//
////                streamBuff.get(bytes) ;
////                streamBytes = new byte[streamBuff.remaining()]; // makes byte array large enough to hold image
////                streamBuff.get(streamBytes); // copies image from buffer to byte array
//                try {
//                    output = new FileOutputStream(file);
//                    output.write(bytes);    // write the byte array to file
//                    //j++;
//                    //success = true;
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Error(0): Ensure App has Write Access to Storage", Toast.LENGTH_LONG).show();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Error(1): Ensure App has Write Access to Storage", Toast.LENGTH_LONG).show();
//                } finally {
//                    image.close(); // close this to free up buffer for other images
//                    if (null != output) {
//                        try {
//                            output.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//            }
//       // }
//
//    }
    private byte[] processImageNew(Image image) {
        ByteBuffer buffer;
        byte[] bytes = new byte[0];
        //FileOutputStream output = null;

        if (image.getFormat() == ImageFormat.JPEG) {
            buffer = image.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
            buffer.get(bytes); // copies image from buffer to byte array
            image.close(); // close this to free up buffer for other images
        }
        // }

        return bytes;
    }
    public static Bitmap rotate(float x, Bitmap source) {
        Bitmap bitmapOrg = source;

        int width = bitmapOrg.getWidth();

        int height = bitmapOrg.getHeight();

        int newWidth = 279;

        int newHeight = 270;

        // calculate the scale - in this case = 0.4f

        float scaleWidth = ((float) newWidth) / width;

        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();

        matrix.postScale(scaleWidth, scaleHeight);
        matrix.postRotate(x);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);

        //iv.setScaleType(ImageView.ScaleType.CENTER);
        //iv.setImageBitmap(resizedBitmap);
        return resizedBitmap;
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    protected CaptureRequest createCaptureRequest() {
        String msg = "0";
        try {
            //m_cnt++;
            //if (m_cnt <=1) {
            //CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            //CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            msg = "1";
            builder.addTarget(imageReader.getSurface());
            msg = "2";
            builder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.SHADING_MODE, CameraMetadata.SHADING_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.HOT_PIXEL_MODE, CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 15); //jpeg image quality
            //builder.set(CaptureRequest.JPEG_QUALITY, (byte) 100); //jpeg image quality
            return builder.build();
            //}
        } catch (CameraAccessException e) {
            //Log.e("[DEBUG]", "createCaptureRequest : " + msg + " : " + m_cnt + " : " + e.getMessage());
            return null;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
