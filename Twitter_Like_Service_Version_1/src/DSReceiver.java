import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DSReceiver implements Runnable {
	int timeLimit=10000;  //10s to update the list of IPs
	private HashSet<String> localIps;
	//keep the ip address and its number of announcement.
	private ConcurrentHashMap<InetAddress,Integer> backendIPs;
	private ConcurrentHashMap<InetAddress,Integer> frontendIPs;
	
	public DSReceiver(HashSet<String> localIps, ConcurrentHashMap<InetAddress,Integer> backendIPs, ConcurrentHashMap<InetAddress,Integer> frontendIPs) {
		this.localIps = localIps;
		this.backendIPs = backendIPs;
		this.frontendIPs = frontendIPs;
	}

	@Override
	public void run() {
		System.out.println("NewAnnounceReceiver");
		try {
			DatagramSocket serverSocket = new DatagramSocket(8888);
			byte[] receiveData = new byte[1024];
			while(true)
			{
				//!!!clear data received from last message
				Arrays.fill(receiveData, (byte)0);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String sentence = new String(receivePacket.getData()).trim();
				InetAddress IPAddress = receivePacket.getAddress();
				// System.out.println(IPAddress.getHostAddress());
				if(localIps.contains(IPAddress.getHostAddress()))
					continue;
				System.out.println("RECEIVED from " + IPAddress.getHostAddress() + ": " + sentence);
				/* if IPs doesn't have the ip, which means it is dynamically lauched(not in the first place), 
				 *iniate its count to 5, to make sure(assume) it is counted live */
				if (DiscoveryService.ServerType.valueOf(sentence).equals(DiscoveryService.ServerType.BACKEND)) {
					if(!backendIPs.keySet().contains(IPAddress)) {
						backendIPs.put(IPAddress, 5);
					}else {
						backendIPs.put(IPAddress, backendIPs.get(IPAddress)+1);
					}
				} else {
					if(!frontendIPs.keySet().contains(IPAddress)) {
						frontendIPs.put(IPAddress, 5);
					}else {
						frontendIPs.put(IPAddress, frontendIPs.get(IPAddress)+1);
					}
				}
			}
		} catch (Exception ex) {
		}
	}
}