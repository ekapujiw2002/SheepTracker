import java.io.*;
import java.net.*;
import java.util.ArrayList;

class TankServer implements Runnable{

	String clientSentence;
	String capitalizedSentence;
	ServerSocket serverSocket;
	
	ArrayList<Thread> threads = new ArrayList<Thread>();
	
	private boolean running;
	public TankServer(SheepTest parent){
		try {
			serverSocket = new ServerSocket(4444);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.out.println("Server thread started");
		}
	}
	
	
	public void run(){

		while(true)
		{
			Socket connectionSocket;
			try {
				connectionSocket = serverSocket.accept();
				Thread t = new Thread(new TankServerThread(this,connectionSocket));
				threads.add(t);
				
				t.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}