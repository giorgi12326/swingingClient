package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class UdpSender {


    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(1313);

        byte[] data = ("SYN").getBytes();

        InetAddress ip = InetAddress.getByName("192.168.1.20");
        DatagramPacket packet =
                new DatagramPacket(data, data.length, ip, 1234);

        socket.send(packet);
        socket.receive(packet);

        data = packet.getData();
        String synAck = new String(data);
        System.out.println(synAck);
        if(synAck.equals("SYN-ACK")){
            byte[] syn = ("ACK").getBytes();
            packet.setData(syn);
            socket.send(packet);
        }

        Thread.sleep(16);

    }
}
