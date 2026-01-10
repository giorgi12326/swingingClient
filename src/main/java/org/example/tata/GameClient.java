package org.example.tata;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class GameClient {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverIp = InetAddress.getByName("127.0.0.1");

        Scanner scanner = new Scanner(System.in);

        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    System.out.println(new String(packet.getData(), 0, packet.getLength()));
                }
            } catch (Exception e) {}
        }).start();

        while (true) {
            String msg = scanner.nextLine();
            byte[] data = msg.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverIp, 5555
            );
            socket.send(packet);
        }
    }
}
