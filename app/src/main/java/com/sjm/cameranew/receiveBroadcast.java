package com.sjm.cameranew;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class receiveBroadcast {
    private DatagramSocket receiveSocket;
    private String sViewerIP;

    public String getsViewerIP() {
        return sViewerIP;
    }

    public void setsViewerIP(String sViewerIP) {
        this.sViewerIP = sViewerIP;
    }

    public  receiveBroadcast() {
        try {
            receive();
            //return sViewerIP;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //return null;
    }
    private DatagramSocket getReceiveSocket() throws UnknownHostException, SocketException {
        if (receiveSocket == null) {
            receiveSocket = new DatagramSocket(8002, InetAddress.getByName("0.0.0.0")); // 0.0.0.0 for listen to all ips
            receiveSocket.setBroadcast(true);
        }
        return receiveSocket;
    }

    public void receive() throws IOException {
        // Discovery request command
        byte[] buffer = "              ".getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        getReceiveSocket().receive(packet);
        System.out.println("Discovery package received! -> " + packet.getAddress() + ":" + packet.getPort());
        // Get received data
        String data = new String(packet.getData()).trim();
        sViewerIP = data;
        if (data.equals("__DISCOVERY_REQUEST__")) { // validate command
            // Send response
            byte[] response = new byte["__DISCOVERY_RESPONSE".length()];
            DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
            getReceiveSocket().send(responsePacket);
            System.out.println("Response sent to: " + packet.getAddress() + ":" + packet.getPort());
        } else {
            System.err.println("Error, not valid package!" + packet.getAddress() + ":" + packet.getPort());
        }
    }
}
