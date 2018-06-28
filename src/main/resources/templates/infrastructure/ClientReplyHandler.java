package $infrastructure.javaPackage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * This class defines the common behaviour of all 
 * Client-side request / reply handlers. 
 * 
 * It provides a method that blocks waiting for a reply
 * using a timeout 
 * 
 * It is a generic class that is instantiated for the specific Reply type for each 
 * Request / Reply communications pattern 
 *  
 * @author Petros Pissias
 *
 */
public class ClientReplyHandler <ReplyType>{

	//the reply queue. This is used in order to read a reply following the send of a request
	private final BlockingQueue<ReplyType> replyQ = new LinkedBlockingQueue <ReplyType>();
	
	//the timeout to wait for a reply
	private final int timeoutSeconds;
	
	
	public ClientReplyHandler(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	/**
	 * Returns the reply of the specific request.
	 * @return the reply object
	 * @throws RemoteCommunicationsException if no reply is received within a timeout or if the communication channel is interrupted
	 */
	public ReplyType getReply() throws RemoteCommunicationsException{
		Object reply;
		try {
			reply = replyQ.poll(timeoutSeconds, TimeUnit.SECONDS);
					
			if (reply == null) {
				throw new RemoteCommunicationsException(RemoteCommunicationsErrorType.TIMEOUT, "Did not receive a reply for "+timeoutSeconds+" seconds");
			}
		} catch (InterruptedException e) {			
			throw new RemoteCommunicationsException(RemoteCommunicationsErrorType.DISCONNECTED, "Interrupted while waiting for a reply because of underlying communication channel problems");
		}
		
		return (ReplyType)reply;		
	}
	


	/**
	 * Inserts a reply that was received
	 * @param reply the reply
	 * @throws InterruptedException in case the reply queue throws an exception
	 */
	public void insertReply(ReplyType reply) throws InterruptedException {
		replyQ.put(reply);
	}
}
