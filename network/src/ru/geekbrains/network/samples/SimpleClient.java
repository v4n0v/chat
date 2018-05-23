package ru.geekbrains.network.samples;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClient {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 8189)){
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                out.writeUTF(sc.nextLine());
                System.out.println(in.readUTF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
