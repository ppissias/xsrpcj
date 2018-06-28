package $infrastructure.javaPackage;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

#if ($infrastructure.logging == "log4j")
import org.apache.log4j.Logger;
#end
/**
 * This thread reads data from a Socket either passing it to a DataHandler
 * or indicates a communication error to an ErrorHandler 
 * 
 * @author Petros Pissias
 *
 */
public class SocketDataTransceiverReaderThread extends Thread{

	#if ($infrastructure.logging == "log4j")
	//logger
	private final Logger logger = Logger.getLogger(getClass());
	#end
	
	//the socket transceiver
	private final Socket mySocket;
	
	//the Socket input stream to read data from
	private final DataInputStream inputInterface;
	
	//reference to an error handler that ocmmunication errors are reported to
	private final ErrorHandler errorHandler;
	
	//reference to a data handler that receives decoded data
	private final DataHandler dataHandler;

	//thread number counter
	private static int threadCount = 0; 

	/**
	 * Constructs a new reader thread
	 * @param mySocket the socket to read data from
	 * @param errorHandler the error handler to report communication errors
	 * @param dataHandler the dat ahandler where decoded data is forwarded
	 * @throws IOException in case of communication problems with the provided socket
	 */
	public SocketDataTransceiverReaderThread(Socket mySocket, ErrorHandler errorHandler, DataHandler dataHandler) throws IOException {
		super(SocketDataTransceiverReaderThread.class.getSimpleName()+"-"+getThreadCount()+" remote host:"+mySocket.getInetAddress()+" port:"+mySocket.getPort());
		this.mySocket = mySocket;
		this.errorHandler = errorHandler;
		this.dataHandler = dataHandler;
		this.inputInterface = new DataInputStream(mySocket.getInputStream());
	}

	@Override
	public void run() {
		//read data until there is a communications error
		while (true) {
			try {
				#if ($infrastructure.logging == "log4j")
				logger.debug("Reading packet...");
				#end
				
				//read payload size
				int payloadSize = inputInterface.readInt();			
				#if ($infrastructure.logging == "log4j")
				logger.debug("Incoming packet. Header decoded. Payload length:"+payloadSize);
				#end
				
				//read payload
				byte[] payload = new byte[payloadSize];
				inputInterface.readFully(payload);

				#if ($infrastructure.logging == "log4j")
				logger.debug("Payload read completed");
				#end
				
				dataHandler.handleDataIndication(payload);
				#if ($infrastructure.logging == "log4j")
				logger.debug("Packet read complete & dispatched");
				#end
			} catch (IOException e) {
				#if ($infrastructure.logging == "log4j")
				logger.error("Communications error. Indicating communications problem to handler and terminating. Error info:"+e.getMessage());
				#end
				errorHandler.handleCommunicationsError();
				
				#if ($infrastructure.logging == "log4j")
				logger.debug("Terminating...");
				#end
				return;
			}
		}
	} 
	
	//helper method to get a thread count for the name of the tread
	private static synchronized int getThreadCount() {
		if (threadCount == Integer.MAX_VALUE) {
			threadCount = 0;
		}
		threadCount = threadCount + 1;
		return threadCount;
	}

}
