import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/*announce every 2s, check every 10s, if the count of a IP <5, then it is dead. Else add it to list of live IP.
 * the IPs will be reset(to 0) every 10s, so it make sure, every 10s will have a list of live IPs*/

public class DSCheck implements Runnable {
	private static final int checkTime = 10000; //check every 10s to return a list of live IPs

	private DiscoveryService service; 
	private ConcurrentHashMap<InetAddress,Integer> backendIPs;
	private ConcurrentHashMap<InetAddress,Integer> frontendIPs;
	private InetAddress primaryIP;  //updated by BullyService

	public DSCheck(DiscoveryService service, InetAddress primaryIP, ConcurrentHashMap<InetAddress,Integer> backendIPs, ConcurrentHashMap<InetAddress,Integer> frontendIPs) {
		this.service = service;
		this.primaryIP = primaryIP;
		this.backendIPs = backendIPs;
		this.frontendIPs = frontendIPs;
	}

	@Override
	public void run() {
		while(true){
			//let
			try {
				Thread.sleep(checkTime);
				System.out.println("DSCheck checking...");
				System.out.println("Primary IP: " + primaryIP.getHostAddress());
				ArrayList<InetAddress> deadIPs = new ArrayList<InetAddress>();
				for(InetAddress i : backendIPs.keySet()){
					if(backendIPs.get(i) < 4) {
						backendIPs.remove(i);
						deadIPs.add(i);
					}
					else
						backendIPs.put(i,  0);  //reset counter of current valid IP to 0, and wait for next 10s to check new valid IPs.
				}
				for(InetAddress i : frontendIPs.keySet()){
					if(frontendIPs.get(i) < 4) {
						frontendIPs.remove(i);
					}
					else
						frontendIPs.put(i,  0);  //reset counter of current valid IP to 0, and wait for next 10s to check new valid IPs.
				}
				if(!deadIPs.isEmpty() && (deadIPs.contains(primaryIP))) {
					service.startElect();   
				}
				//For Testing discovery service: 
				System.out.println("Print out valid IPs after each DSCheck...");
				for(InetAddress i:backendIPs.keySet()){
					System.out.println(i);
				}
				for(InetAddress i:frontendIPs.keySet()){
					System.out.println(i);
				}
				System.out.println("getIPs size: " + service.getBackendIPs().size());
			} catch (InterruptedException e) {
				System.out.println("Exception in DSCheck's run");
			}	
		}
	}
	
	public void setPrimary(InetAddress primaryIP) {
		this.primaryIP = primaryIP;
	}
}
