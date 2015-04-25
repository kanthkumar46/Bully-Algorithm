package com.rit.bully;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

class FailureDetector extends Thread{
	final static String GROUP_ADDR = "224.0.9.10";
	final static int GROUP_PORT = 8000;
	public static Map<String,Process> processList = new HashMap<>();
	
	MulticastSocket mcSocket;
	public FailureDetector() {
		try {
			mcSocket = new MulticastSocket(GROUP_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		System.setProperty("java.net.preferIPv4Stack" , "true");
		try {
			InetAddress inetAddress = InetAddress.getByName(GROUP_ADDR);
			mcSocket.joinGroup(inetAddress);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Timer heartBeatTimer = new Timer();
		heartBeatTimer.schedule(new HeartBeat(), 0, 5 * 1000);
		
		byte[] buf = new byte[1024];
		Runnable HeartBeatHandler = new Runnable() {
			@Override
			public void run() {
				while(true){
					try {
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						mcSocket.receive(packet);
						new HeartBeatWorker(packet).start();
					
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		Thread thread = new Thread(HeartBeatHandler);
		thread.start();
		
		Timer processVerifier = new Timer();
		processVerifier.schedule(new ProcessVerifier(), 15 * 1000, 15 * 1000);
		
	}
	
}


class HeartBeatMessage implements Serializable{
	private static final long serialVersionUID = 1L;
	
	String processID;
	String ipAddress;
	
	public HeartBeatMessage() {
		try {
			this.processID = Bully.MY_PROCESS_ID;
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}


class HeartBeatSerializeUtil {
	public static byte[] serialize(HeartBeatMessage msg){
		ByteArrayOutputStream baos = null;
		
		try {
			baos = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(baos);
			os.writeObject(msg);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return baos.toByteArray();
	}
	
	public static HeartBeatMessage deSerialize(byte[] buf){
		HeartBeatMessage msg = null;
		
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			ObjectInputStream ois = new ObjectInputStream(bais);
			msg = (HeartBeatMessage)ois.readObject();
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return msg;
	}
}


class HeartBeat extends TimerTask{
	public void run() {
		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(FailureDetector.GROUP_ADDR);
			byte[] msg = HeartBeatSerializeUtil.serialize(new HeartBeatMessage());
			
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(msg, msg.length,inetAddress, 
					FailureDetector.GROUP_PORT);
			socket.send(packet);
			socket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
}


class Process{
	String processID;
	String ipAddress;
	long timeStamp;
	
	public Process(String PID, String address) {
		this.processID = PID;
		this.ipAddress = address;
		this.timeStamp = System.currentTimeMillis();
	}
}


class HeartBeatWorker extends Thread{
	DatagramPacket packet;
	
	public HeartBeatWorker(DatagramPacket packet) {
		this.packet = packet;
	}
	
	@Override
	public void run() {
		byte[] buf = packet.getData();
		HeartBeatMessage msg = HeartBeatSerializeUtil.deSerialize(buf);
		
		synchronized (FailureDetector.processList) {
			if(FailureDetector.processList.containsKey(msg.processID)){
				Process process = FailureDetector.processList.get(msg.processID);
				process.timeStamp = System.currentTimeMillis();
				FailureDetector.processList.put(msg.processID,process);
			}
			else{
				Process process = new Process(msg.processID, msg.ipAddress);
				FailureDetector.processList.put(msg.processID,process);
			}
		}
		
		if(Integer.parseInt(msg.processID) > 
			Integer.parseInt(Bully.LEADER_PROCESS_ID))
			Bully.LEADER_PROCESS_ID = msg.processID;
		
	}
}


class ProcessVerifier extends TimerTask{
	
	@Override
	public void run() {
		if(!Bully.ELECTION_FLAG){
			synchronized (FailureDetector.processList) {
				Map<String, Process> processList = FailureDetector.processList;
				
				/*System.out.println("Process Verifier : ");
				System.out.println("--------------------------------------------------------------");
				for (Process process : processList.values())
					System.out.println(process.ipAddress + " | "
							+ process.processID + " | " + process.timeStamp);
				System.out.println("\n");*/
				
				Iterator<Entry<String, Process>> it = processList.entrySet().iterator();
						
				while(it.hasNext()){
					Map.Entry<String, Process> entry = it.next();
					Process process = entry.getValue();
					long diff = System.currentTimeMillis() - process.timeStamp;
					if((diff > 5 * 1000) && 
							process.processID.equals(Bully.LEADER_PROCESS_ID)){
						System.out.println("\nLeader Failed: Process ID is " + process.processID);
						Bully.startElection();
					}
				}
			}
		}
	}
	
}
