package ro.streamrelayserver;

import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.springframework.stereotype.Component;

@Component
public class Server extends JFrame {

	private static final long serialVersionUID = 1L;

	private JLabel myLabel;
	
	private volatile String sharedImageAsString = "";
	
	private AtomicInteger ai = new AtomicInteger(0);

	public Server() throws IOException {

		myLabel = new JLabel(
				new ImageIcon("C:\\Users\\tiber\\Pictures\\RKyaEDwp8J7JKeZWQPuOVWvkUjGQfpCx_cover_580.jpg"));

		add(myLabel);

		// Setting Frame width and height
		setSize(608, 480);

		// Setting the title of Frame
		setTitle("This is my First AWT example");

		// Setting the layout for the Frame
		setLayout(new FlowLayout());

		/*
		 * By default frame is not visible so we are setting the visibility to true to
		 * make it visible.
		 */
		setVisible(true);

		ExecutorService es = null;
		try {
			es = Executors.newCachedThreadPool();

			es.submit(() -> handleStreamFromRaspberryPi());

			es.submit(() -> handleRabbitMQMessageCommands());
			
			listenForAndroidClientConnectionAndDispatchToThread(es);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (es != null) {
				es.shutdown();
			}
		}

	}

	private void listenForAndroidClientConnectionAndDispatchToThread(ExecutorService es) throws IOException {
		ServerSocket ss = new ServerSocket(8001);
		
		// running infinite loop for getting
		// client request
		while (true) {
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();

				System.out.println("A new client is connected : " + s);

				// obtaining input and out streams
				// DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				System.out.println("Assigning new thread for this client");

				// Invoking the start() method
				es.submit(() -> handleClientRequestFromAndroid(dos));

			} catch (Exception e) {
				s.close();
				ss.close();
				e.printStackTrace();
			}
		}
	}

	private void handleRabbitMQMessageCommands() {
		while(true) {
			try {
				System.out.println(ai.get());
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void handleClientRequestFromAndroid(DataOutputStream dos) {
		System.out.println("Assigned thread " + Thread.currentThread().getName());
		ai.incrementAndGet();
		
		try {
			while(true) {
				dos.writeUTF(sharedImageAsString); 
			}
		} catch(SocketException se) {
			
			ai.decrementAndGet();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	private void handleStreamFromRaspberryPi() {

		ServerSocket welcomeSocket = null;
		try {
			welcomeSocket = new ServerSocket(8000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {

			while (true) {
				Socket connectionSocket = welcomeSocket.accept();

				Reader r = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

				int i = 0;
				int intch;

				char[] buffer = new char[2000000];

				char[] charArr;

				while ((intch = r.read()) != -1) {
					char c = (char) intch;

					if (c == '*') {

						charArr = new char[i];

						for (int j = 0; j < i; j++) {
							charArr[j] = buffer[j];
						}

						sharedImageAsString = new String(charArr);

						myLabel.setIcon(new ImageIcon(Base64.getDecoder().decode(sharedImageAsString)));

						buffer = new char[2000000];
						i = 0;
					} else {
						if (!Character.isLetter(c) && !Character.isDigit(c) && c != '+' && c != '/' && c != '=') {
							System.out.println("Current character is invalid! " + c);
							continue;
						}

						buffer[i] = c;
						i++;
					}

				}
			}

		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			try {
				welcomeSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
