package cardgame;

import java.net.ServerSocket;
import java.io.*;
import java.net.*;

public class Server {
	
	//Global variables
	static ServerSocket serverSocket;
	static Socket socket;
	static DataOutputStream out;
	static DataInputStream in;
	static Users[] user = new Users[10];
	
	//Main method
	public static void main(String[] args) throws Exception {
		System.out.println("Starting server...");
		serverSocket = new ServerSocket(7777);
		System.out.println("Server started!");
		while(true){
			socket = serverSocket.accept();
			for(int i=0; i<10; i++){ 
				if(user[i]==null){
					System.out.println("Connection from: " + socket.getInetAddress());
					out = new DataOutputStream(socket.getOutputStream());
					in = new DataInputStream(socket.getInputStream());
					user[i] = new Users(out, in, user); 
					Thread thread = new Thread(user[i]); //if a user accepts, it will get a thread value, the first gets 0, second gets 1,...
					thread.start();
					break;
				}
			}
		}
	}
}

//Seperate class file, same imports
class Users implements Runnable{
	
	//Global variables
	DataOutputStream out;
	DataInputStream in;
	Users[] user = new Users[10];
	String name;
	
	//Essentials of this class file that always have to be given if this class file is being called
	public Users(DataOutputStream out, DataInputStream in, Users[] user){
		this.out = out;
		this.in = in;
		this.user = user;
	}

	public void run() {
		try {
			name = in.readUTF(); //receive the name value from client
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while(true){
			try {
				String message = in.readUTF(); //Receive the message from client
				for(int i=0; i<10;i++){
					if(user[i] != null){
						user[i].out.writeUTF(name + ":" + message); //Send the name and message to all connected clients
					}
				}
			} catch (IOException e) { //part of the error catching, gets called if a client disconnects
				this.out = null;
				this.in = null;
			}
		}
	}
	
}
