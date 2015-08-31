import java.net.InetAddress;
import java.net.UnknownHostException;

//Running BackEnd
public class Driver {
	public static void main(String[] args){
		InetAddress primaryIP = null;
		try {
			primaryIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Open a Discovery Service for Each BE server.
		BackEnd be = new BackEnd(12346, primaryIP);
		be.start();
		
		/*
		InetAddress addr1 = null, addr2 = null;		
		try {
			addr1 = InetAddress.getByName("10.0.0.1");
			addr2 = InetAddress.getByName("10.0.0.25");
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		TestRunner runner;
		runner = new TestRunner(addr1);
		Thread thread = new Thread(runner);
		thread.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		runner.setAddr(addr2);
		*/
	}
}
