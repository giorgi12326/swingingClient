package org.example.tata;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class GameServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(5555);
        byte[] buffer = new byte[1024];

        Map<SocketAddress, String> players = new HashMap<>();

        System.out.println("Server started on port 5555");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            SocketAddress addr = packet.getSocketAddress();
            String msg = new String(packet.getData(), 0, packet.getLength());

            players.putIfAbsent(addr, "Player" + players.size());
            System.out.println(players.get(addr) + ": " + msg);

            // Echo back
            String reply = "Server got: " + msg;
            byte[] out = reply.getBytes();

            DatagramPacket response = new DatagramPacket(
                    out, out.length,
                    packet.getAddress(), packet.getPort()
            );
            socket.send(response);
        }
    }
}
