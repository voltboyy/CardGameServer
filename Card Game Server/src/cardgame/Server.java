package cardgame;

import java.net.ServerSocket;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Server {
	
	//Global variables
	static ServerSocket serverSocket;
	static Socket socket;
	static DataOutputStream out;
	static DataInputStream in;
	byte[] salt, encrpass;
	static Authenticate[] authUser = new Authenticate[100];
	public boolean[] userExists = new boolean[100];
	
	//Main method
	public static void main(String[] args) throws Exception {
		new Server();
	}
	
	public Server() throws Exception{
		System.out.println("Starting server...");
		serverSocket = new ServerSocket(49500);
		System.out.println("Server started!");
		while(true){
			socket = serverSocket.accept();
			for(int i=0; i<100; i++){
				if(authUser[i]==null){ //Apparently user[i] is always null
					System.out.print("Connection from: " + socket.getInetAddress());
					System.out.println(" PID " + i);
					out = new DataOutputStream(socket.getOutputStream());
					in = new DataInputStream(socket.getInputStream());
					authUser[i] = new Authenticate(out, in, authUser, i); 
					Thread thread = new Thread(authUser[i]); //if a user accepts, it will get a thread value, the first gets 0, second gets 1,...
					thread.start();
					break;
				}
			}
		}
	}
}

//Seperate class file, same imports
class Authenticate implements Runnable{

	DataOutputStream out;
	DataInputStream in;
	Authenticate[] authUser = new Authenticate[100];
	String givenUser = "", givenCode = "", givenMail = "", givenPass = "9";
	String username;
	String rawpass;
	String usernameinput;
	int[] usernumber = new int[100];
	int levelInt;
	int playerid;
	int playeridin;
	int xin;
	int yin;
	byte[] salt, encrpass;
	public boolean[] succes = new boolean[100];
	public boolean[] userExists = new boolean[100];
	public boolean[] codeUsed = new boolean[100];
	
	public Authenticate(DataOutputStream out, DataInputStream in, Authenticate[] auth, int pid){
		this.out = out;
		this.in = in;
		this.authUser = auth;
		this.playerid = pid;
	}
	
	public boolean authenticate(String attemptedPassword, byte[] encryptedPassword, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Encrypt the clear-text password using the same salt that was used to
		// encrypt the original password
		byte[] encryptedAttemptedPassword = getEncryptedPassword(attemptedPassword, salt);
		// Authentication succeeds if encrypted password that the user entered
		// is equal to the stored hash
		return Arrays.equals(encryptedPassword, encryptedAttemptedPassword);
	}
	public byte[] getEncryptedPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// PBKDF2 with SHA-1 as the hashing algorithm. Note that the NIST
		// specifically names SHA-1 as an acceptable hashing algorithm for PBKDF2
		String algorithm = "PBKDF2WithHmacSHA1";
		// SHA-1 generates 160 bit hashes, so that's what makes sense here
		int derivedKeyLength = 160;
		// Pick an iteration count that works for you. The NIST recommends at
		// least 1,000 iterations:
		// http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf
		// iOS 4.x reportedly uses 10,000:
		// http://blog.crackpassword.com/2010/09/smartphone-forensics-cracking-blackberry-backup-passwords/
		int iterations = 20000;
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength);
		SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
		return f.generateSecret(spec).getEncoded();
	}
	public byte[] generateSalt() throws NoSuchAlgorithmException {
		// VERY important to use SecureRandom instead of just Random
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		// Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
		byte[] salt = new byte[8];
		random.nextBytes(salt);
		return salt;
	}
	
	public void run() {
		try {
			levelInt = in.readInt();
			usernumber[playerid] = 0;
			if(levelInt == 1){
				System.out.println("PID " + playerid + " is authenticating.");
				username = in.readUTF();
				rawpass = in.readUTF();
				Scanner scanuser = new Scanner(new File("C:/GameServer/user.txt"));
				succes[playerid] = false;
				while(scanuser.hasNextLine()){
					usernumber[playerid]++;
					String user = scanuser.nextLine();
					if(username.equals(user)){
						//This works like a charm
						byte[] saltbytes = Files.readAllBytes(Paths.get("C:/GameServer/ServerData/"+username+"-salt.dat"));
						byte[] passbytes = Files.readAllBytes(Paths.get("C:/GameServer/ServerData/"+username+"-pass.dat"));
						if(authenticate(rawpass, passbytes, saltbytes)){
							out.writeBoolean(true);
							succes[playerid] = true;
							//System.out.print("Connection from: " + socket.getInetAddress());
							System.out.println("PID " + playerid + " connected!");
							try { //Sends playerid to client
								out.writeInt(playerid);
							} catch (IOException e1) {
								System.out.println("Failed to send PlayerID");
							}
							while(true){//Receives all information needed from client
								try{
				            		playeridin = in.readInt();
									xin = in.readInt();
									yin = in.readInt();
									usernameinput = in.readUTF();
				            		for(int i=0; i<100;i++){ //Sends the gathered information to all connected clients, correctly
										if(authUser[i] != null){
											authUser[i].out.writeInt(playeridin);
											authUser[i].out.writeInt(xin);
											authUser[i].out.writeInt(yin);
											authUser[i].out.writeUTF(usernameinput);
										}
									}
				            		Thread.sleep(10);
								}catch(IOException | InterruptedException e){ //part of the error catching, gets called if a client disconnects
									System.out.println("PID " + playerid + " disconnected");
									authUser[playerid] = null;
									for(int i=0; i<100;i++){ //Sends the gathered information to all connected clients, correctly
										if(authUser[i] != null){
											authUser[i].out.writeInt(playerid);
											authUser[i].out.writeInt(0);
											authUser[i].out.writeInt(0);
											authUser[i].out.writeUTF("");
										}
									}
									break; //break moet hier zeker staan samen met de null want anders refreshed die ni als een player disconnects waardoor een id constant ingenomen blijft.
								}
							}
							break;
						}
					}
				}
				if(!succes[playerid]){
					out.writeBoolean(false);
					System.out.println("PID " + playerid + " disconnected");
					System.out.println("Error in 'succes'");
					authUser[playerid] = null;
				}
			}else if(levelInt == 2){
				System.out.println("PID " + playerid + " is trying to create an account...");
				givenCode = in.readUTF();
				givenUser = in.readUTF();
				givenPass = in.readUTF();
				givenMail = in.readUTF();
				System.out.println("[PID " + playerid + "] "+givenCode);
				System.out.println("[PID " + playerid + "] "+givenUser);
				System.out.println("[PID " + playerid + "] "+givenMail);
				Scanner scancode = new Scanner(new File("C:/GameServer/code.txt"));
				userExists[playerid] = false;
				codeUsed[playerid] = false;
				if(givenCode.isEmpty() || givenUser.isEmpty() || givenPass.isEmpty() || givenMail.isEmpty()){
					System.out.println("PID " + playerid + " hasn't filled in all fields...");
					out.writeBoolean(false);
				}else{
					System.out.println("PID " + playerid + " has filled in all fields...");
					out.writeBoolean(true);
					while(scancode.hasNextLine()){
						String code = scancode.nextLine();
						if(givenCode.equals(code)){
							Scanner scanusedcodes = new Scanner(new File("C:/GameServer/usedcodes.txt"));
							while(scanusedcodes.hasNextLine()){
								String usedcode = scanusedcodes.nextLine();
								if(givenCode.equals(usedcode)){
									codeUsed[playerid] = true;
									break;
								}
							}
							out.writeBoolean(codeUsed[playerid]);
							if(!codeUsed[playerid]){
								System.out.println("[PID " + playerid + "] Code exists and is unused, proceeding with username check...");
								Scanner scanuser = new Scanner(new File("C:/GameServer/user.txt"));
								while(scanuser.hasNextLine()){
									String user = scanuser.nextLine();
									if(givenUser.equals(user)){
										System.out.println("Username exists");
										userExists[playerid] = true;
										break;
									}
								}
							}else{
								System.out.println("[PID " + playerid + "] Code has been used, breaking loop...");
								break;
							}
							out.writeBoolean(userExists[playerid]);
							if(!userExists[playerid] && !codeUsed[playerid]){
								System.out.println("[PID " + playerid + "] Username available, proceeding with account creation...");
								try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/GameServer/user.txt", true)))) {
								    out.println(givenUser);
								}catch (IOException e1) {
								    //exception handling left as an exercise for the reader
								}
								try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/GameServer/mails.txt", true)))) {
								    out.println(givenMail);
								}catch (IOException e1) {
								    //exception handling left as an exercise for the reader
								}
								try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/GameServer/usedcodes.txt", true)))) {
								    out.println(givenCode);
								}catch (IOException e1) {
								    //exception handling left as an exercise for the reader
								}
								try {
									salt = generateSalt();
									FileOutputStream fos = new FileOutputStream("C:/GameServer/ServerData/"+givenUser+"-salt.dat");
									fos.write(salt);
									fos.close();
								} catch (NoSuchAlgorithmException | IOException e1) {
									e1.printStackTrace();
								}
								try {
									byte[] saltbytes = Files.readAllBytes(Paths.get("C:/GameServer/ServerData/"+givenUser+"-salt.dat"));
									encrpass = getEncryptedPassword(givenPass, saltbytes);
									FileOutputStream fos = new FileOutputStream("C:/GameServer/ServerData/"+givenUser+"-pass.dat");
									fos.write(encrpass);
									fos.close();
								} catch (FileNotFoundException e2) {
									e2.printStackTrace();
								} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							break;
						}
					}
					out.writeBoolean(true);
				}
				givenPass = "";
				authUser[playerid] = null;
			}
		} catch (IOException e1) {
			System.out.println("[PID " + playerid + "] Failed to get authentification data...");
			System.out.println("PID " + playerid + " disconnected");
			System.out.println(e1);
			authUser[playerid] = null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			authUser[playerid] = null;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			authUser[playerid] = null;
		}
	}
	
}
