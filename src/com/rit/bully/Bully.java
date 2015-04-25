package com.rit.bully;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Bully {
	public static String MY_PROCESS_ID;
	public static volatile String LEADER_PROCESS_ID = "0";
	ServerSocket serverSoc = null;
	public final static int MESSAGE_PORT = 8001;
	
	public static volatile boolean OK_FLAG = false;
	public static volatile boolean LEADER_FLAG = false; 
	public static volatile boolean ELECTION_FLAG = false;
	
	public Bully(String processID) {
		MY_PROCESS_ID = processID;
		try{
			serverSoc =  new ServerSocket(MESSAGE_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	Runnable MessageHandler = new Runnable() {
		@Override
		public void run() {
			while(true){
				Socket clientSoc;
				try {
					clientSoc = serverSoc.accept();
					new MessageWorker(clientSoc).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public static void main(String[] args) {
		if(args.length != 1){
			System.err.println("Usage : java Bully <Process ID>");
			System.exit(0);
		}
		
		Bully bully = new Bully(args[0]);
		new FailureDetector().start();
		
		Thread thread = new Thread(bully.MessageHandler);
		thread.start();
		
		System.out.println("Press Ctrl-C to terminate the process\n");
	}
	
	public static void startElection(){
		System.out.println("Initiating Election");
		Map<String, Process> processList = FailureDetector.processList;
		for (Process process : processList.values()){
			if(Integer.parseInt(process.processID) > Integer.parseInt(MY_PROCESS_ID)){
				try {
					Socket socket = new Socket(process.ipAddress, Bully.MESSAGE_PORT);
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(Messages.Election);
					out.writeObject(new HeartBeatMessage());
					out.close();
					socket.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}
		
		Timer AckVerifier = new Timer();
		AckVerifier.schedule(new AckVerifier(), 10 * 1000);
	}
	
	public static void declareAsLeader(){
		Map<String, Process> processList = FailureDetector.processList;
		for (Process process : processList.values()){
			try {
				Socket socket = new Socket(process.ipAddress, Bully.MESSAGE_PORT);
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(Messages.Leader);
				out.writeObject(new HeartBeatMessage());
				out.close();
				socket.close();
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
	}
	
}


class MessageWorker extends Thread{
	Socket clientSoc;
	
	public MessageWorker(Socket socket) {
		this.clientSoc = socket;
	}
	
	@Override
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(clientSoc.getInputStream());
			Messages msg = (Messages)in.readObject();
			switch (msg) {
			case OK: handleOKMessage((HeartBeatMessage) in.readObject());
				break;
			case Election: handleElection((HeartBeatMessage) in.readObject());
				break;
			case Leader: handleLeaderMessage((HeartBeatMessage) in.readObject());
				break;
			}
			in.close();
			clientSoc.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void handleLeaderMessage(HeartBeatMessage processInfo) {
		System.out.println("Message Received COORDINATOR from process "+processInfo.processID);
		Bully.LEADER_PROCESS_ID = processInfo.processID;
		Bully.LEADER_FLAG = true;
		Bully.ELECTION_FLAG = false;
		System.out.println("New Leader :"+Bully.LEADER_PROCESS_ID );
	}

	private void handleElection(HeartBeatMessage processInfo) {
		Bully.ELECTION_FLAG = true;
		System.out.println("Message Received ELECTION from process "+processInfo.processID);
		try {
			Socket socket = new Socket(processInfo.ipAddress, Bully.MESSAGE_PORT);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(Messages.OK);
			out.writeObject(new HeartBeatMessage());
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Bully.startElection();
	}

	private void handleOKMessage(HeartBeatMessage processInfo) {
		System.out.println("Message Received OK from process "+processInfo.processID);
		Bully.OK_FLAG = true;
	}
	
}


class AckVerifier extends TimerTask{

	@Override
	public void run() {
		if (Bully.OK_FLAG){
			Timer LeaderVerifier = new Timer();
			LeaderVerifier.schedule(new LeaderVerifier(), 20 * 1000);
			Bully.OK_FLAG = false;
		}
		else{
			Bully.declareAsLeader();
		}
	}

}


class LeaderVerifier extends TimerTask{

	@Override
	public void run() {
		if (Bully.LEADER_FLAG){
			Bully.LEADER_FLAG = false;
		}
		else{
			System.out.println("COORDINATOR message not received Re-Election started \n");
			Bully.startElection();
		}
	}

}