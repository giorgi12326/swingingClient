package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class UdpSender {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();

        byte[] data = "Hello via UDP".getBytes();
        InetAddress ip = InetAddress.getByName("82.211.163.67");

        DatagramPacket packet =
                new DatagramPacket(data, data.length, ip, 1234);

        socket.send(packet);
        socket.close();

    }
}
