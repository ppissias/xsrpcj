package $infrastructure.javaPackage;

import java.io.IOException;

#if ($infrastructure.logging == "log4j")
import org.apache.log4j.Logger;
#end

/**
 * This abstract class implements the common functions 
 * of all concrete Service implementations on the client side.
 * 
 * It has an internal SocketDataTransceiver object which is used
 * for the low-level packet based communication with the server.
 * 
 * Before each RPC interaction, all concrete Service
 * implementations will call the checkDataTransceiver
 * method ensuring that there is a valid SocketDataTransceiver object
 * 
 * This class receives error notifications from the SocketDataTransceiverReaderThread 
 * when a communications problem is detected and clears the SocketDataTransceiver
 * in order to establish a new communications channel for the next interaction
 * 
 * @author Petros Pissias
 *
 */
public abstract class ServiceProxy implements ErrorHandler{

	//the server host
	protected final String host;
	
	//the server port
	protected final int port;
	
	//the data transceiver instance
	protected SocketDataTransceiver clientDataTransceiver = null; 

	#if ($infrastructure.logging == "log4j")
	//logger
	private final Logger logger = Logger.getLogger(getClass());
	#end
	
	public ServiceProxy(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * This method is called as the first statement of each RPC call handling
	 * and it ensures that we will have a valid SocketDataTransceiver
	 * for the duration of the call. 
	 * 
	 * If a valid SocketDataTransceiver instance cannot be created, it will throw an exception 
	 * 
	 * The caller method is synchronized so that it will not interfere with 
	 * the handleCommunicationsError method, which clears (nullifies) the 
	 * SocketDataTransceiver following a communication error.
	 * 
	 * The actual SocketDataTransceiver instance is provided by the Service implementation
	 * class, as it has access to the appropriate DataHandler and ErrorHandler objects.
	 * 
	 * @throws RemoteCommunicationsException If a valid SocketDataTransceiver instance cannot be created
	 */
	protected void checkDataTransceiver() throws RemoteCommunicationsException {
		try {		
			if (clientDataTransceiver == null ) {
				#if ($infrastructure.logging == "log4j")
				logger.debug("creating data transceiver");
				#end
				clientDataTransceiver = getDataTransceiverInstance();
				clientDataTransceiver.initialize();
			}
		}
		catch (IOException e) {
			#if ($infrastructure.logging == "log4j")
			logger.error("Exception trying to connect to server", e);
			#end
			throw (new RemoteCommunicationsException(RemoteCommunicationsErrorType.CANNOT_CONNECT, e.getMessage()));
		} 			
	}
	
	/**
	 * This method is called when there is a communications error
	 * either from the reader side (SocketDataTransceiverReaderThread) or
	 * from the service implementation, when a send() fails.
	 * 
	 * It will have the effect that the SocketDataTransceiver object 
	 * will be cleared (nullified) and a new object will be created for the next interaction.
	 * 
	 * The method is synchronized in order not to interfere with the Service
	 * implementation methods, which are also synchronized
	 */
	@Override	
	public synchronized void handleCommunicationsError() {
		if (clientDataTransceiver == null) {
			#if ($infrastructure.logging == "log4j")
			logger.debug("Communications error indication already handled. clientDataTransceiver is null");
			#end
		} else {
			//check from where the call originates
			if (Thread.currentThread() instanceof SocketDataTransceiverReaderThread) {
				//check if the call we are handling originates from the actual clientDataTransceiver instance we have
				if (clientDataTransceiver.isDataTransceiverThread(Thread.currentThread())) {
					//this call originates from the current reader thread
					#if ($infrastructure.logging == "log4j")
					logger.error("Received communications error indication. Clearing data transceiver");
					logger.debug("Received communications error indication from SocketDataTransceiverReaderThread. Clearing data transceiver");
					#elseif ($infrastructure.logging == "System")
					System.out.println("Error: Received communications error indication");					
					#end
					
					clientDataTransceiver = null;
				} else {
					//in this case, we received an error indication from a previous clientDataTransceiver
					//object (race condition). The indication is already handled from an exception and a already have 
					//a new instance
					#if ($infrastructure.logging == "log4j")
					logger.warn("Received a call from a previous SocketDataTransceiverReaderThread instance. The error indication already handled.");
					#end
				}				
			} else {
				//we received an error indication from a service implementation 
				#if ($infrastructure.logging == "log4j")
				logger.error("Received communications error indication. Clearing data transceiver");
				logger.debug("Received communications error indication from a service implementation. Clearing data transceiver");
				#end
				clientDataTransceiver = null;				
			}
		}
	}	
	
	/**
	 * this method should be called when a client no longer wants to interact with a server.
	 * It will release any resources that the client service implementation uses.
	 * Synchronized because we must ensure we hold a valid instance of a socket data transceiver during the operation
	 * @return true if all resources were successfully disposed, false if there was an exception during the release of the resources
	 */
	public synchronized boolean finished() {
		if (clientDataTransceiver != null) {
			return clientDataTransceiver.closeSocket();
		}
		return true;
	}	
	/**
	 * Implemented by concrete implementations.
	 * @return a new instance of a SocketDataTransceiver
	 * @throws IOException 
	 */
	protected abstract SocketDataTransceiver getDataTransceiverInstance() throws IOException ;
	
}
