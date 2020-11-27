package com.sjm.camera;

import android.Manifest;
import android.app.Service;
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
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.os.Handler;

public class Camera2Service extends Service {
    protected static final int CAMERA_CALIBRATION_DELAY = 500;
    protected static final String TAG = "myLog";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected String m_ipaddress = "";
    protected String m_filename = "";
    protected String m_Orientation = "PORTRAIT";
    protected String m_Tolerance = "15.5";
    protected String m_eMail = "";
    protected String m_eMailPW = "";
    protected String m_eMailNum = "7";
    protected String m_ringtone = "";
    protected Boolean m_allwaysNotify = false;
    protected Boolean m_sendMail = true;
    protected CaptureRequest.Builder builder = null;
    protected int m_cnt = 0;
    protected int m_MillisecFramePeriod = 200;
    private Handler handler;
    private int PHOTOWIDTH = 220;//320;
    private int PHOTOHEIGHT = 220; //320;

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
                //Log.e("[DEBUG]", "CameraCaptureSession : "  + e.getMessage());
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
                        processImage(img);
                        Runnable r = new sendImageNew();   //Send Image
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
                        //Log.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }
//    class test extends Thread{
//        @Override
//        public void run() {
//            //Log.d("tttt", "test class thread is      >"+Thread.currentThread().getName());
//        }
//    }

//    class readyCameraThread extends Thread {
//
//        public void run() {
//            Log.d(TAG, "inside readyCameraThread");
//            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
//            try {
//                String pickedCamera = getCamera(manager);
//                if (ActivityCompat.checkSelfPermission(Camera2Service.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
//                manager.openCamera(pickedCamera, cameraStateCallback, null);
//                imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
//                imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
//                Log.d(TAG, "imageReader created");
//            } catch (CameraAccessException e){
//                Log.e(TAG, e.getMessage());
//            }
//        }
//    }

    /********************************************************************************************************************/
    /********************************************************************************************************************/
//    public void readyCamera() {
//        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
//        try {
//            String pickedCamera = getCamera(manager);
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
//            manager.openCamera(pickedCamera, cameraStateCallback, null);
//            imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
//            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
//            Log.d(TAG, "imageReader created");
//        } catch (CameraAccessException e){
//            Log.e(TAG, e.getMessage());
//        }
//    }
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
            @SuppressWarnings("deprecation")
            String fPath = Environment.getExternalStorageDirectory()
                    + File.separator + "generated" + File.separator;
            /*********************************************************************/
            File myDir;

            myDir = new File(fPath);
            if (!myDir.isDirectory()) {
                myDir.mkdirs();
            }
            /*********************************************************************/
            m_filename = fPath + "dummy.jpg";
            //Log.d(TAG, "FILENAME : " + m_filename);

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
        //Log.e("[DEBUG]","onCreate Camera2Service");
        handler = new Handler();
        super.onCreate();
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
    public void onDestroy() {
        //Log.d("[DEBUG]", "Camera2Service OnDestroy " );
        try {
            session.abortCaptures();
        } catch (CameraAccessException e) {
            //Log.e(TAG, "Camera2Service OnDestroy abortCaptures ERROR0: " + e.getMessage());
            e.printStackTrace();
        }
        try {

            session.close();
            //Log.d("[DEBUG]", "OnDestroy Closed session" );
        } catch (Exception e) {
            //Log.e(TAG, "Camera2Service OnDestroy Session Close ERROR0: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public class sendImageNew implements Runnable {
        ByteBuffer buffer;
        byte[] bytes;
        boolean success = false;
        DatagramSocket ds = null;
        int Angle = 0;
        File imagefile = null;

        public sendImageNew() {
            // store parameter for later user
        }

        public void run() {

            try {

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    m_Orientation = "PORTRAIT";
                } else {
                    m_Orientation = "LANDSCAPE";
                }

                imagefile = new File(m_filename);
                FileInputStream fis = null;

                try {
                    // outputStream = clientSocketImage.getOutputStream();
                    fis = new FileInputStream(imagefile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Bitmap bm = BitmapFactory.decodeStream(fis);

                // rotate stream image 90 degrees (so server displays bitmap correctly)
                if (m_Orientation.trim().equals("PORTRAIT"))
                    Angle = 90;
                else
                    Angle = 0;

                Bitmap rotatedImg = rotate(Angle, bm); // rotate(bm,filename);

                bytes = getBytesFromBitmap(rotatedImg);
                // Prepare DatagramSocket
                ds = new DatagramSocket();
                buffer = ByteBuffer.wrap(bytes);

                bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                buffer.get(bytes); // copies image from buffer to byte array
                DatagramPacket DpSend =
                        new DatagramPacket(bytes, bytes.length, InetAddress.getByName(m_ipaddress), 8080);

                ds.send(DpSend);
                fis.close();
                ds.close();


            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.d("[DEBUG]", "[AFTER DATAGRAM SEND]: " + bytes.length);
            //imagefile.delete();
            //ds.close();

        }
    }

    public class sendDataNew implements Runnable {
        //private OutputStream outputStreamMsg;
        private String sErr = "";
        private DatagramSocket ds;
        //int packetcnt = 0;
        private byte[] databyte;

        public sendDataNew() {
            // store parameter for later user
        }

        public void run() {
            String sOrientation = "";

            try {
                //runnerCam = new AsyncTaskRunnerCam();
                //runnerCam.execute("[DEBUG] DATA THREAD -  TRYING TO GET SOCKET]");

                ds = new DatagramSocket();

            } catch (IOException e) {
                sErr += "[00]" + e;
                //try {
                //    if (ds != null)
                //        ds.close();
                //} catch (Exception ex) {
                //    ex.printStackTrace();
                //}
                e.printStackTrace();
                //Log.d("[DEBUG]", "[ERROR(dat - thread)]]: " + e);
                //Intent intent = getIntent();
                //intent.putExtra("Status","Failed");
                //setResult(RESULT_OK, intent);
                //finish();
                return;
            } catch (Exception e) {

                e.printStackTrace();
                //Log.d("[DEBUG]", "[ERROR(222)]]: " + e);
            }
            try {
                //int Angle = 0;
                // rotate stream image 90 degrees (so server displays bitmap correctly)
                //Log.d("[DEBUG]", "Camera2Service sendData m_Orientation: " + m_Orientation);
                if (m_Orientation.trim().equals("PORTRAIT")) {
                    //Angle = 90;
                    sOrientation = "~PORTRAIT";
                } else {
                    //Angle = 0;
                    sOrientation = "~LANDSCAPE";
                }
                //String outStr = m_Tolerance + sOrientation;
                String outStr = m_Tolerance + sOrientation + "~" + m_eMail + "~" + m_eMailPW + "~" + m_eMailNum + "~" + m_sendMail + "~" + m_ringtone + "~" + m_allwaysNotify + "~terminator";
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

            //if (sErr != "") {
            //Log.d("[DEBUG]", "[ERROR(Data Thread)]]: " + sErr);
            //}
            //UpdateMsg("Data Thread Connected to Server...", (int)Color.GREEN  );

        }

    }

    private void processImage(Image image) {
        //Process image data
        ByteBuffer buffer;
        byte[] bytes;
        //boolean success = false;

        //File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/image.jpg");

        File file = new File(m_filename);

        FileOutputStream output = null;

        if (image.getFormat() == ImageFormat.JPEG) {
            buffer = image.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
            buffer.get(bytes); // copies image from buffer to byte array
            try {
                output = new FileOutputStream(file);
                output.write(bytes);    // write the byte array to file
                //j++;
                //success = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close(); // close this to free up buffer for other images
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }


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
