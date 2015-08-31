import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
/* TODO: The reason why I use multicast in Discovery Service: 
 * If I pre-configure current primary when I launch a new backEnd server, 
 * if change to a new primary, the pre-configuration information has to be changed too, and it is not efficient.
 * The trade-off is I can only announce in the same subnet.  
 * */

public class DSAnnouncer implements Runnable {
	public static final String BROADCAST = "255.255.255.255"; 
	private static final int port = 8888;
	private InetAddress broadcastIP = null;
	private DiscoveryService.ServerType serverType;
	
	public DSAnnouncer(DiscoveryService.ServerType serverType) {
		this.serverType = serverType;
	}
	
	@Override
	public void run() {
		DatagramSocket clientSocket;
		try {
			clientSocket = new DatagramSocket();
			clientSocket.setBroadcast(true);
			serviceAnnounce(clientSocket, serverType.toString().getBytes(), BROADCAST, port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void serviceAnnounce(DatagramSocket clientSocket, byte[] sendData,
			String BoradcastIP, int port2) {
		try {
			broadcastIP = InetAddress.getByName(BROADCAST);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, broadcastIP, port);
				clientSocket.send(sendPacket);
				//DS announce every 2s.
				Thread.sleep(2000);  
				System.out.println(clientSocket.getClass().getName()
						+ ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
				}catch (Exception e) {
				System.err.println(e.toString());
			}
		}

	}
}
