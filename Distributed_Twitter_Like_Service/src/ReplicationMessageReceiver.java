import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ReplicationMessageReceiver implements Runnable {
	private Flag flag;
	
	public ReplicationMessageReceiver(Flag flag) {
		this.flag = flag;
	}

	@Override
	public void run() {
		System.out.println("ReplicationMessageReceiver: start running");
		try {
			DatagramSocket socket = new DatagramSocket(BackEnd.REPLICATE_ANNOUNCE_PORT);
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(receivePacket);
			flag.setFlag(true);
			socket.close();
			System.out.println("ReplicationMessageReceiver: received replication complete message");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
