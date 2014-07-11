package cardgame;

import java.net.ServerSocket;
import java.io.*;
import java.net.*;

public class Server {
	
	static ServerSocket serverSocket;
	static Socket socket;
	static DataOutputStream out;
	
	public static void main(String[] args) throws Exception {
		System.out.println("Starting server...");
		serverSocket = new ServerSocket(7777);
		System.out.println("Server started!");
		socket = serverSocket.accept();
		System.out.println("Connection from: " + socket.getInetAddress());
		out = new DataOutputStream(socket.getOutputStream());
		out.writeUTF("Test test");
		System.out.println("Data has been sent");
	}
	
}
