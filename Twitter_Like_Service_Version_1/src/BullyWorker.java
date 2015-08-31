import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/* this worker is to handle logic of bully algorithms:
 *  
 *  electMsg      sender(when to send #)     receiver(what to do after receive #)
 *  1.start      1.startElect()   			          1.send "ok"				  
 *  			 2.receive "start"					  2. null(if "I" have smaller ID.)
 *  
 *  2.ok		 receive "start"					  null(do nothing)
 *  
 *  3.elected	 Ns after start without receiving ok  1.setPrimary()	
 *													  2.startElect() -->bully
 */

public class BullyWorker extends Worker {
	private BullyService service;
	private DiscoveryService.ServerType serverType;

	//Bully worker will ask for Discovery service for the current living IPs.
	public BullyWorker(Socket clientSocket, BullyService service, DiscoveryService.ServerType serverType) {
		super(clientSocket);
		this.service = service;
		this.serverType = serverType;
	}

	@Override  //Override Worker's constructResponse for bully algorithm's "receiver" logic.
	protected Response constructResponse(Request request) {
		try {
			InetAddress localIP = InetAddress.getLocalHost();
			//Since request's host is the receiver's IP , so use clientSocket to get remote(sender)'s IP.
			//BullyWorker inherits clientSocket from Worker, and then get remote IP from clientSocket.
			InetAddress remoteIP = InetAddress.getByName(clientSocket.getInetAddress().getHostAddress());
			//if there are higher ID's IPs, send electMessage. Or announce myself as the new leader. 

			//when receiving election start message. 
			if(request.body.equals(BullyService.startMsg)){
				//if localIP's priority is higher than senderIP's priority,return "ok". else do nothing.   
				if(hasHigherPriority(localIP,remoteIP) && serverType == DiscoveryService.ServerType.BACKEND){
					service.startElect();
					return new Response(200, "OK", BullyService.okMsg);
				}
			}
			else if(request.body.equals(BullyService.electedMsg)){
				if (serverType == DiscoveryService.ServerType.BACKEND) {
					//called when a new leader has been elected by Bully sender/receiver.then primaryIP has been updated and can pass to DS.
					if(hasHigherPriority(remoteIP,localIP)){
						service.setPrimary(remoteIP);
					}else {  //be "bully" if new coordinator's ID is lower than me.
						service.startElect();
					}
				} else {
					service.setPrimary(remoteIP);
				}
			}
			//do nothing when receiving the higher ID's "ok" message.
		} catch (UnknownHostException e) {
			System.out.println("exception in constructResponse() in BullyWorker");
			return new Response(500, "Internal Error", "");
		}
		return new Response(200, "OK", "MSG RECEIVED");
	}

	public boolean hasHigherPriority (InetAddress localIP,InetAddress i) {
		String ip = i.toString();
		int id = Integer.parseInt(ip.substring(ip.lastIndexOf('.')+1, ip.length()));
		String localip = localIP.toString();
		int idOfLocalIP = Integer.parseInt(localip.substring(localip.lastIndexOf('.')+1, localip.length()));
		return idOfLocalIP > id ? true:false;
	}
}

