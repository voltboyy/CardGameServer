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
		new Server();
		
	}
	
	public Server() throws Exception{
		System.out.println("Starting server...");
		serverSocket = new ServerSocket(49500);
		//DataPing dp = new DataPing(out, this);
		//dataping = new Thread(dp);
		//dataping.start();
		System.out.println("Server started!");
		while(true){
			socket = serverSocket.accept();
			for(int i=0; i<10; i++){ 
				if(user[i]==null){
					System.out.print("Connection from: " + socket.getInetAddress());
					System.out.println(" PID " + i);
					out = new DataOutputStream(socket.getOutputStream());
					in = new DataInputStream(socket.getInputStream());
					user[i] = new Users(out, in, user, i); 
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
	int playerid;
	int playeridin;
	int xin;
	int yin;
	
	//Essentials of this class file that always have to be given if this class file is being called
	public Users(DataOutputStream out, DataInputStream in, Users[] user, int pid){
		this.out = out;
		this.in = in;
		this.user = user;
		this.playerid = pid;
	}

	public void run() {
		try { //Sends playerid to client
			out.writeInt(playerid);
		} catch (IOException e1) {
			System.out.println("Failed to send PlayerID");
		}
		while(true){
			try { //Receives all information needed from client
				playeridin = in.readInt();
				xin = in.readInt();
				yin = in.readInt();
				for(int i=0; i<10;i++){ //Sends the gathered information to all connected clients, correctly
					if(user[i] != null){
						user[i].out.writeInt(playeridin);
						user[i].out.writeInt(xin);
						user[i].out.writeInt(yin);
					}
				}
			} catch (IOException e) { //part of the error catching, gets called if a client disconnects
				System.out.println("PID " + playerid + " disconnected");
				user[playerid] = null;
				break; //break moet hier zeker staan samen met de null want anders refreshed die ni als een player disconnects waardoor een id constant ingenomen blijft.
			}
		}
	}
	
}
