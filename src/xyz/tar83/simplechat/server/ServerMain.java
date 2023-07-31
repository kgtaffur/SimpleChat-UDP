package xyz.tar83.simplechat.server;

public class ServerMain {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -jar name.jar [port]");
			return;
		}
		int port = Integer.parseInt(args[0]);
		new Server(port);
	}
}
