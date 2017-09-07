package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	private int port;
	private ExecutorService executor;
	private HashMap<String,ClientHandler> clientList;
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
		clientList = new HashMap<String,ClientHandler>();
	}

	public void run() {
		log.info("server started");
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			while (true) {
				Socket socket = ss.accept();
				String tempName = makeTempName();
				ClientHandler handler = new ClientHandler(socket,this,tempName);
				executor.execute(handler);
				clientList.put(tempName,handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	private String makeTempName()	
	{
		boolean nameIsUnique = false;
		String tempName = "";
		int random;
		
		do{
			random = (int)(Math.random() * Integer.MAX_VALUE);
			tempName = random+"";
			if(!clientList.containsKey(tempName))
				nameIsUnique = true;
		
		}while(!nameIsUnique);
		
		return tempName;
	}
	
	public String updateUsername(String oldName,String newName)
	{
		ClientHandler ch = clientList.remove(oldName);
		if(!clientList.containsKey(newName))
		{
			clientList.put(newName, ch);	
			return newName;
		}
		else	//Handle the case where multiple users try to use the same username by appending a number to the second user's name
		{
			int counter = 1;
			String realNewName;
			do{
				realNewName = newName + "(" + counter + ")";
				counter++;
			}while(clientList.containsKey(realNewName));
			
			clientList.put(realNewName, ch);
			return realNewName;
		}
		
	}
	
	public void sendWhisper(String recipient,Message m)
	{
		m.setContents(getTimeStamp() + " <" + m.getUsername() + "> " + "(whisper): " +m.getContents());
		clientList.get(recipient).sendToClient(m);
	}
	
	public void sendBroadcast(String sender,Message m)
	{		
		m.setContents(getTimeStamp() + " <" + m.getUsername() + "> " + "(all): " +m.getContents());
		
		for(String n : clientList.keySet())
		{
			//System.out.println(n);
			if(n.equals(sender))
			{	
				continue;
			}
			else clientList.get(n).sendToClient(m);			
		}
	}
	
	public Message remove(String n,Message m)
	{
		m.setContents(getTimeStamp() + " <" + m.getUsername() + "> " + " has disconnected");
		clientList.remove(n);
		return m;
	}
	
	public Message echo(Message m)
	{
		m.setContents(getTimeStamp() + " <" + m.getUsername() + "> " + "(echo): " +m.getContents());
		return m;
	}
	
	public String getFormattedClientList()
	{
		String temp = getTimeStamp() + " currently connected users: ";
		
		for(String n : clientList.keySet())
		{
			temp+= ("\n" + n);
		}
		
		return temp;
	}
	
	private String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(cal.getTime());
	}

}
