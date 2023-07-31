package xyz.tar83.simplechat.client;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class ClientWindow extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;

	private JTextField txtMessage;
	private JTextArea history;

	private Client client;
	private boolean running = false;

	public ClientWindow(String name, String address, int port) {
		setTitle("Simple Chat Client");
		client = new Client(name, address, port);
		
		boolean isConnected = client.openConnection(address);
		if (!isConnected) {
			System.err.println("Connection failed!");
			console("Connection failed!");
		}

		createWindow();
		welcome();
		
		String msg = "/c/" + name + " connected from " + client.getSocket().getLocalAddress().toString() + ":"
				+ client.getSocket().getLocalPort();
		client.send(msg.getBytes());
		new Thread(this, "Window").start();
	}

	private void welcome() {
		console("Attempting a connection to " + client.getAddress() + ":" + client.getPort() + " as " + client.getName()
				+ "...");
		console("Successful connection!");
	}

	@Override
	public void run() {
		this.running = true;
		listen();
	}

	private void createWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(750, 480);
		setLocationRelativeTo(null);
		setMinimumSize(new Dimension(750, 480));
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);

		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 35, 685, 30, 0 };
		gbl_contentPane.rowHeights = new int[] { 62, 410, 8 };
		gbl_contentPane.columnWeights = new double[] { 1.0, 1.0 };
		gbl_contentPane.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		history = new JTextArea();
		history.setEditable(false);
		history.setLineWrap(true);
		history.setWrapStyleWord(true);
		JScrollPane scroll = new JScrollPane(history);
		GridBagConstraints scrollConstraints = new GridBagConstraints();
		scrollConstraints.insets = new Insets(0, 0, 5, 5);
		scrollConstraints.fill = GridBagConstraints.BOTH;
		scrollConstraints.gridx = 0;
		scrollConstraints.gridy = 0;
		scrollConstraints.gridwidth = 3;
		scrollConstraints.gridheight = 2;
		contentPane.add(scroll, scrollConstraints);

		txtMessage = new JTextField();
		txtMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendMessage();
				}
			}
		});
		GridBagConstraints gbc_txtMessage = new GridBagConstraints();
		gbc_txtMessage.insets = new Insets(0, 0, 0, 5);
		gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMessage.gridx = 0;
		gbc_txtMessage.gridy = 2;
		gbc_txtMessage.gridwidth = 2;
		contentPane.add(txtMessage, gbc_txtMessage);
		txtMessage.setColumns(10);

		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		GridBagConstraints gbc_btnSend = new GridBagConstraints();
		gbc_btnSend.insets = new Insets(0, 0, 0, 5);
		gbc_btnSend.gridx = 2;
		gbc_btnSend.gridy = 2;
		contentPane.add(btnSend, gbc_btnSend);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				String disconnect = "/d/" + client.getID();
				client.send(disconnect.getBytes());
				running = false;
				client.close();
			}
		});

		setVisible(true);

		txtMessage.requestFocusInWindow();
	}

	private void sendMessage() {
		String msg = txtMessage.getText();
		if (!msg.trim().equals("")) {
			msg = "/m/" + client.getName() + ": " + msg;
			client.send(msg.getBytes());
		}
		txtMessage.setText("");
	}

	public void listen() {
		new Thread("Listen") {
			@Override
			public void run() {
				while (running) {
					String message = client.receive();
					if (message.startsWith("/c/")) {
						client.setID(Integer.parseInt(message.trim().substring(3)));
						console("Successfully connected to server! Your ID is " + client.getID());
					} else if (message.startsWith("/m/")) {
						console(message.substring(3));
					} else if (message.startsWith("/p/")) {
						String str = "/p/" + client.getID();
						client.send(str.getBytes());
					}
				}
			}
		}.start();
	}

	public void console(String message) {
		history.append(message + "\n");
		this.history.setCaretPosition(this.history.getDocument().getLength());
	}
}
