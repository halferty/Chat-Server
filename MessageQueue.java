package my.halferty.chatserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;

public class MessageQueue implements Runnable {
	private Hashtable<String, QueueMessage> messages = new Hashtable<String, QueueMessage>();
	private SecureRandom random = new SecureRandom();
	Connection conn;
	PreparedStatement prep, prep2;
	Statement stat, stat2;
	
	public MessageQueue() {
		try {
			Class.forName("org.sqlite.JDBC");
			
			// Create the database if it doesn't exist.
			File dbFile = new File("chatdb1.db");
			if (!dbFile.exists()) {
				System.out.println("Creating chatdb1.db file");
				dbFile.createNewFile();
				System.out.println("Creating messages table");
				conn = DriverManager.getConnection("jdbc:sqlite:chatdb1.db");
				stat = conn.createStatement();
				stat.executeUpdate("create table messages (ufrom, uto, umessage, urandomkey);");
			}
			
			// Set up the database queries
			conn = DriverManager.getConnection("jdbc:sqlite:chatdb1.db");
			prep = conn.prepareStatement("insert into messages values (?, ?, ?, ?);");
			prep2 = conn.prepareStatement("delete from messages where uto=?;");
			stat = conn.createStatement();
			
			// Load the data from SQLite
			ResultSet rs = stat.executeQuery("select * from messages;");
			
			// Load the data into the hashtable
		    while (rs.next()) {
		    	messages.put(rs.getString("urandomkey"), new QueueMessage(rs.getString("ufrom"), rs.getString("uto"), rs.getString("umessage")));
		    	System.out.println("From " + rs.getString("ufrom") + " To " + rs.getString("uto") + " Message " + rs.getString("umessage") + " Unique Key " + rs.getString("urandomkey"));
		    }
		    rs.close();
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(true) {
			if(!messages.isEmpty()) {
				// Send all
				Enumeration<String> keys = messages.keys();
				while( keys.hasMoreElements() ) {
				  Object key = keys.nextElement();
				  QueueMessage message = messages.get(key);
				  if (ChatServer.loggedInUsers.containsKey(message.getTo())) {
					  
					  // Send the message every 10 seconds until the client acknowledges
					  System.out.println("Lazy writer attempting to send a message to user " + message.getTo());
					  PrintWriter dout = new PrintWriter(ChatServer.outputStreams.get(ChatServer.loggedInUsers.get(message.getTo())));
					  dout.println("incoming_message " + message.getFrom() + " " + message.getMessage() + " " + key);
					  dout.flush();
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void AddMessage(String from, String to, String message) {
		String randomKey = new BigInteger(130, random).toString(32);
		messages.put(randomKey, new QueueMessage(from, to, message));
	    try {
	    	prep.setString(1, from);
			prep.setString(2, to);
			prep.setString(3, message);
			prep.setString(4, randomKey);
			prep.addBatch();
		    conn.setAutoCommit(false);
		    prep.executeBatch();
		    conn.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
	
	public boolean ClearMessage(String key, String username) {
		if (messages.containsKey(key)) {
			messages.remove(key);
			try {
				prep2.setString(1, username);
				prep2.addBatch();
			    conn.setAutoCommit(false);
			    prep2.executeBatch();
			    conn.setAutoCommit(true);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}
}

class QueueMessage {
    private String from, to, message;
    public QueueMessage(String from, String to, String message){
        this.from = from;
        this.to = to;
        this.message = message;
    }
    public String getFrom(){ return from; }
    public String getTo(){ return to; }
    public String getMessage(){ return message; }
}

