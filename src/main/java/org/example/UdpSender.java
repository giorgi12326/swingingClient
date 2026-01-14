package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class UdpSender {


    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(1313);

        byte[] data =  new byte[4096];
        for (int i = 0; i < 4096; i++) {
            data[i] = (byte)i;
        }

        InetAddress ip = InetAddress.getByName("192.168.0.102");
        DatagramPacket packet =
                new DatagramPacket(data, data.length, ip, 1234);
while(true) {
    socket.send(packet);
}

    }
}
