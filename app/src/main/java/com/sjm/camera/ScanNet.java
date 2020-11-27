package com.sjm.camera;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ScanNet {
    Context m_context;

    public static int[] rangeFromCidr(String cidrIp) {
        int maskStub = 1 << 31;
        String[] atoms = cidrIp.split("/");
        int mask = Integer.parseInt(atoms[1]);
        System.out.println(mask);

        int[] result = new int[2];
        try {
            result[0] = InetRange.ipToInt(atoms[0]) & (maskStub >> (mask - 1)); // lower bound
            result[1] = InetRange.ipToInt(atoms[0]); // upper bound
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("[DEBUG RANGE(0)]" + InetRange.intToIp(result[0]));
        //System.out.println("[DEBUG RANGE(1)]" + InetRange.intToIp(result[1]));

        return result;
    }

    public List main(String subnet, Context context) throws Exception {

        //final  List<String> iparraylist = new ArrayList<>();
        final int ind = 0;
        m_context = context;
        Boolean bNetAvailable = isNetworkAvailable();
        List IPs = getNetworkIPs(subnet);
        Log.d("[DEBUG BREAK]", "[DEBUG FINISHED SCAN]==> ");
        return IPs;
    }

    //    public class MyRunnableIP implements Runnable {
//        private int cnt;
//        public MyRunnableIP(int cnt) {
//            this.cnt = cnt;
//        }
//
//        public void run() {
//            cnt+=1;
//        }
//    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) m_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static class InetRange {
        public static int ipToInt(String ipAddress) {
            try {
                byte[] bytes = InetAddress.getByName(ipAddress).getAddress();
                int octet1 = (bytes[0] & 0xFF) << 24;
                int octet2 = (bytes[1] & 0xFF) << 16;
                int octet3 = (bytes[2] & 0xFF) << 8;
                int octet4 = bytes[3] & 0xFF;
                int address = octet1 | octet2 | octet3 | octet4;

                return address;
            } catch (Exception e) {
                e.printStackTrace();

                return 0;
            }
        }

        public static String intToIp(int ipAddress) {
            int octet1 = (ipAddress & 0xFF000000) >>> 24;
            int octet2 = (ipAddress & 0xFF0000) >>> 16;
            int octet3 = (ipAddress & 0xFF00) >>> 8;
            int octet4 = ipAddress & 0xFF;

            return new StringBuffer().append(octet1).append('.').append(octet2)
                    .append('.').append(octet3).append('.')
                    .append(octet4).toString();
        }
    }

    public List<String> getNetworkIPs(String sub) {
        final byte[] ip;
        final String[] ipList = new String[0];
        final List<String> iparraylist = new ArrayList<>();
        //final boolean bDone = false;
        String sRange = sub + ".255/24";
        //int[] bounds = ScanNet.rangeFromCidr("192.168.0.255/24");
        int[] bounds = ScanNet.rangeFromCidr(sRange);

        final String subnet = sub; //"192.168.0";
        final boolean noIPs = false;
        for (int i = 1; i < 255; i++) {
            final int j = i;
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {   // new thread for parallel execution
                public void run() {
                    String host = subnet + "." + j;
                    try {

                        if (InetAddress.getByName(host).isReachable(5000)) {
                            final InetAddress ip = InetAddress.getByName(host);
                            iparraylist.add(host + " " + ip.getHostName().trim());
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();     // dont forget to start the thread
        }
        return iparraylist;

    }
}

