package my.halferty.chatserver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class ChatServer {
	
	// Constants
	private static final int PORT = 1337;
	
	private ServerSocket ss;
	protected static Hashtable<String, Socket> loggedInUsers = new Hashtable<String, Socket>();
	protected static Hashtable<Socket, DataOutputStream> outputStreams = new Hashtable<Socket, DataOutputStream>();
	protected static MessageQueue messages;
	
	public ChatServer(int port) throws IOException {
		listen(port);
	}
	
	private void listen(int port) throws IOException {
		try {
			ss = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Could not listen on port " + port);
			e.printStackTrace();
		}
		System.out.println("Listening on " + ss);
		while (true) {
			Socket s = ss.accept();
			System.out.println("Connection from " + s);
			new ServerThread(this, s);
		}
	}
	
	protected void AddLoggedIn(String username, Socket s) {
		synchronized(loggedInUsers) {
			loggedInUsers.put(username,  s);
		}
		synchronized(outputStreams) {
			try {
				outputStreams.put(s,  new DataOutputStream(s.getOutputStream()));
			} catch (IOException e) {
				System.out.println("Could not get OutputStream");
				e.printStackTrace();
			}
		}
	}
	
	protected static void RemoveLoggedIn(Socket s) {
		synchronized(loggedInUsers) {
			System.out.println("Removing connection to " + s);
			loggedInUsers.remove(s);
		}
		synchronized(outputStreams) {
			outputStreams.remove(s);
			try {
				s.close();
			} catch (IOException ie) {
				System.out.println("Error closing " + s);
				ie.printStackTrace();
			}
		}
	}
	
	protected static void DropInvalidKeyClient(String username) {
		System.out.println("User sent invalid chat ack key: " + username);
		RemoveLoggedIn(loggedInUsers.get(username));
	}
	
	protected void SendMessage(String username, String message) {
		PrintWriter dout = new PrintWriter(outputStreams.get(loggedInUsers.get(username)));
		dout.println(message);
		dout.flush();
	}
	
	public static void main(String[] args) throws Exception {
		messages = new MessageQueue();
		new Thread(messages).start();
		System.out.println("Running on port " + PORT);
		new ChatServer(PORT);
	}
	
}
