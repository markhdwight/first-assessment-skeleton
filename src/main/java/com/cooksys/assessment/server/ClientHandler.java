package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private Server server;
	private ObjectMapper mapper;
	private BufferedReader reader;
	private PrintWriter writer;
	private String userName;

	public ClientHandler(Socket socket,Server server, String userName) {
		super();
		this.socket = socket;
		this.server = server;
		this.userName = userName;
	}

	public void run() {
		try {

			mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						userName = server.updateUsername(userName, message.getUsername());
						message.setContents(userName + " has connected");
						server.sendBroadcast(userName,message);
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						server.sendBroadcast(userName,server.remove(userName,message));
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						message = server.echo(message);
						sendToClient(message);
						break;
					case "broadcast":
						log.info("user <{}> broadcasted message <{}>", message.getUsername(),message.getContents());
						server.sendBroadcast(userName, message);
						break;
					case "users":
						log.info("user <{}> requested client list",message.getUsername());
						message.setContents(server.getFormattedClientList());
						sendToClient(message);
						break;
					
				}
				if(message.getCommand().charAt(0) == '@')
				{
					log.info("user <{}> sent message to <{}> : <{}>",message.getUsername(),message.getCommand(),message.getContents());
					String reciever = message.getCommand().substring(1);
					server.sendWhisper(reciever,message);
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	public void sendToClient(Message m){
		String response;
		try 
		{	
			response = mapper.writeValueAsString(m);
		} 
		catch (JsonProcessingException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "";
		}
		writer.write(response);
		writer.flush();
	}

}
