package xyz.tar83.simplechat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server implements Runnable {
	private int port;
	
	private DatagramSocket socket;
	
	private boolean running = false;
	
	private List<ServerClient> clients = new ArrayList<ServerClient>();
	private List<Integer> clientResponses = new ArrayList<Integer>();
	
	private final int MAX_ATTEMPTS = 5;

	public Server(int port) {
		this.port = port;
		
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		new Thread(this, "Server").start();
	}

	@Override
	public void run() {
		running = true;
		System.out.println("Server running on port " + this.port);
		manageClients();
		receive();
		
		Scanner scanner = new Scanner(System.in);
		while (running) {
			String str = scanner.nextLine();
			if (str.equals("/clients")) {
				System.out.println("======================= Connected clients =======================");
				for (ServerClient sc : clients) {
					System.out.println(sc.name + "(" + sc.ID + ") " + sc.address + ":" + sc.port);
				}
				System.out.println("======================= Connected clients =======================");
			}
		}
	}

	private void manageClients() {
		new Thread("Manage") {
			@Override
			public void run() {
				while (running) {
					sendToAll("/p/ping");

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					for (int i = 0; i < clients.size(); i++) {
						ServerClient sc = clients.get(i);
						if (!clientResponses.contains(sc.ID)) {
							if (sc.attempt >= MAX_ATTEMPTS) {
								disconnect(sc.ID, false);
							} else {
								sc.attempt++;
							}
						} else {
							clientResponses.remove((Integer) sc.ID);
							sc.attempt = 0;
						}
					}
				}
			}
		}.start();
		
	}
	
	private void receive() {
		new Thread("Receive") {
			@Override
			public void run() {
				while (running) {
					byte[] data = new byte[1024];
					DatagramPacket packet = new DatagramPacket(data, data.length);
					try {
						socket.receive(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
					process(packet);
				}
			}
		}.start();
		
	}
	
	private void process(DatagramPacket packet) {
		String str = new String(packet.getData());
		if (str.startsWith("/c/")) {
			String clientName = str.substring(3, str.indexOf(" "));
			int id = UniqueIdentifier.getIdentifier();
			clients.add(new ServerClient(clientName, packet.getAddress(), packet.getPort(), id));
			System.out
					.println(clientName + " " + packet.getAddress().toString() + ":" + packet.getPort() + " id: " + id);
			String ID = "/c/" + id;
			send(ID.getBytes(), packet.getAddress(), packet.getPort());
		} else if (str.startsWith("/m/")) {
			sendToAll(str);
		} else if (str.startsWith("/d/")) {
			int id = Integer.parseInt(str.trim().substring(3));
			disconnect(id, true);
		} else if (str.startsWith("/p/")) {
			int id = Integer.parseInt(str.trim().substring(3));
			clientResponses.add(id);
		}
	}

	private void disconnect(int id, boolean status) {
		ServerClient sc = null;
		for (ServerClient serverClient : clients) {
			if (serverClient.ID == id) {
				sc = serverClient;
				clients.remove(serverClient);
			}
			break;
		}
		String msg = "";
		if (status) {
			msg = "Client " + sc.name + " (" + sc.ID + ") @ " + sc.address + ":" + sc.port + " disconnected.";
		} else {
			msg = "Client " + sc.name + " (" + sc.ID + ") @ " + sc.address + ":" + sc.port + " timed out.";
		}
		System.out.println(msg);
	}

	private void sendToAll(String msg) {
		for (ServerClient serverClient : clients) {
			send(msg.getBytes(), serverClient.address, serverClient.port);
		}
	}

	private void send(byte[] data, InetAddress address, int port) {
		new Thread("Send") {
			@Override
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
