package my.halferty.chatserver;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Enumeration;

public class ServerThread extends Thread {
	private ChatServer server;
	private Socket socket;
	clientState state;
	private boolean running = true;
	private String username;
	BufferedReader din;
	PrintWriter dout;
	
	public ServerThread(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		state = clientState.login;
		start();
	}
	
	private enum clientState {
		login,command
	}
	
	private void println(String s) {
		dout.println(s + System.getProperty("line.separator"));
		dout.flush();
	}
	
	private void ProcessCommand(String [] splitCommand) {
		String command = splitCommand[0];
		System.out.println("Message recieved: " + command);
		if (command.equals("quit")) {
			System.out.println(username + " disconnected.");
			running = false;
			
		} else if (command.equals("send_message")) {
			if (splitCommand[1] == null) {
				println("send_message error_no_user_specified");
			} else if (splitCommand[2] == null) {
				println("send_message error_no_message_specified");
			} else {
				ChatServer.messages.AddMessage(username, splitCommand[1], splitCommand[2]);
				println("success");
			}
			
		} else if (command.equals("list_users")) {
			Enumeration<String> e = ChatServer.loggedInUsers.keys();
			StringBuilder sb = new StringBuilder("");
		    while(e.hasMoreElements()) {
		      sb.append(" " + e.nextElement());
		    }
		    println("results" + sb.toString());
			
		} else if (command.equals("ack_message")) {
			if (ChatServer.messages.ClearMessage(splitCommand[1], username)) {
			    println("ack_message success");
			} else {
				ChatServer.DropInvalidKeyClient(username);
			}
			
		} else if (command.equals("help")) {
			println("Available commands: quit, send_message <user> <message>, list_users");
		}
	}
	
	public void run() {
		try {
			din = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			dout = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Error opening reader or writer for client stream");
			e.printStackTrace();
		}
		try {
			while(running) {
				switch (state) {
				case login:
					println("enter username:");
					break;
				}
				String message = din.readLine();
				switch (state) {
				case login:
					username = message.replaceAll("[^a-zA-Z0-9]", "");
					if (ChatServer.loggedInUsers.containsKey(username)) {
						System.out.println("Duplicate user " + username + " connected.");
						println("error_already_connected");
						running=false;
					} else {
						System.out.println(username + " connected.");
						server.AddLoggedIn(username, socket);
						state = clientState.command;
					}
					break;
				case command:
					ProcessCommand(message.split("\\s+"));
					break;
				}
			}
		} catch (EOFException ie) {
		} catch (IOException ie) {
			ie.printStackTrace();
		} finally {
			ChatServer.RemoveLoggedIn(socket);
		}
	}
}
