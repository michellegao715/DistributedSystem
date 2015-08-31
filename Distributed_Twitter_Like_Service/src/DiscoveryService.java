import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryService {
	public enum ServerType {
		FRONTEND,
		BACKEND
	}
	//only one instance of IPs which shared by DSReceiver,DSAnnouncer,DSCheck
	private ConcurrentHashMap<InetAddress,Integer> backendIPs;
	private ConcurrentHashMap<InetAddress,Integer> frontendIPs;
	private DSAnnouncer dsannouncer;
	private DSReceiver dsreceiver;
	private DSCheck dscheck;
	private BackEnd backend;
	private InetAddress primaryIP;
	private HashSet<String> localIPs;

	public DiscoveryService(InetAddress primaryIP, ServerType serverType, BackEnd backend) {
		setupLocalIPs();
		this.primaryIP = primaryIP;
		this.backend = backend;
		backendIPs = new ConcurrentHashMap<InetAddress, Integer>();
		frontendIPs = new ConcurrentHashMap<InetAddress, Integer>();
		dsannouncer = new DSAnnouncer(serverType);
		dsreceiver = new DSReceiver(localIPs, backendIPs, frontendIPs);
		dscheck = new DSCheck(this, primaryIP, backendIPs, frontendIPs);
	}

	public void start() {
		Thread serviceAnnounce = new Thread(dsannouncer);
		Thread serviceDiscov = new Thread(dsreceiver);
		Thread serviceCheck = new Thread(dscheck);
		
		serviceAnnounce.start();
		serviceDiscov.start();
		serviceCheck.start();
	}
	
	public void setupLocalIPs() {
		localIPs = new HashSet<String>();
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		while(interfaces.hasMoreElements()) {
			Enumeration<InetAddress> addrs = interfaces.nextElement().getInetAddresses();
			while(addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				if(!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
					System.out.println("Known local ip: " + addr.getHostAddress());
					localIPs.add(addr.getHostAddress());
				}
			}
		}
	}
	
	public boolean isPrimary() {
		return localIPs.contains(primaryIP.getHostAddress());
	}

	public DSCheck getDscheck() {
		return dscheck;
	}

	public Set<InetAddress> getBackendIPs() {
		return backendIPs.keySet();
	}
	
	public Set<InetAddress> getFrontendIPs() {
		return frontendIPs.keySet();
	}
	
	public void startElect() {
		if (backend != null)
			backend.getBS().startElect();
	}
	
	public void setPrimary(InetAddress primaryIP) {
		System.out.println("Set new primary: " + primaryIP.getHostAddress());
		this.primaryIP = primaryIP;
		dscheck.setPrimary(primaryIP);
	}
}
