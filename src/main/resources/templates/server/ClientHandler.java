package ${server.javaPackage}.$serverSubPackage;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
#if ($infrastructure.logging == "log4j")
import org.apache.log4j.Logger;
#end

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

//infrastructure imports
import ${infrastructure.javaPackage}.DataHandler;
import ${infrastructure.javaPackage}.ErrorHandler;
import ${infrastructure.javaPackage}.RemoteCommunicationsErrorType;
import ${infrastructure.javaPackage}.RemoteCommunicationsException;
import ${infrastructure.javaPackage}.SocketDataTransceiver;

//message type imports
#foreach( $importClass in $classImports )
import ${importClass};
#end

//generic message type import
import ${server.javaPackage}.types.${server.name}.MessageContainer;
import ${server.javaPackage}.types.${server.name}.MessageContainer.MessageType;

/**
 * This class handles a connection with a client. 
 * 
 *  When a connection is established with a client an instance of this class is created in order
 *  to handle messages from and to that client. 
 *  
 * 
 * @author Petros Pissias
 *
 */
public class $className implements DataHandler, ErrorHandler#foreach ($callbackInterface in $callbackInterfaces), ${callbackInterface}#end {

	#if ($infrastructure.logging == "log4j")
	private final Logger logger = Logger.getLogger(getClass());
	#end
	
	//the service handler
	private final $serviceInterfaceClassName serviceHandler;
	
	private SocketDataTransceiver serverDataTransceiver;
	
	/**
	 * Constructs a new client handler
	 * @param socket
	 * @throws IOException
	 */
	public ${className}(Socket socket, $serviceInterfaceClassName serviceHandler) throws IOException {
		this.serviceHandler = serviceHandler;
		
		serverDataTransceiver = new SocketDataTransceiver(socket, this, this);		
	}
	
	public void initialize() {
		serverDataTransceiver.initialize();		
	}

	@Override
	//handles incoming data from a client
	public void handleDataIndication(byte[] payload) {
		//new data has arrived 
		#if ($infrastructure.logging == "log4j")
		logger.debug("handling data indication");
		#end
		MessageContainer envelope = null;
		try {
			envelope = MessageContainer.parseFrom(payload);
		} catch (InvalidProtocolBufferException e) {
			#if ($infrastructure.logging == "log4j")
			logger.error("Cannot decode data. Protocol error", e);
			logger.error("Data that cannot be decoded:"+Arrays.toString(payload));
			#elseif ($infrastructure.logging == "System")
			System.out.println("Error: Cannot decode data. Protocol error:"+e.getMessage());
			System.out.println("Error: Data that cannot be decoded:"+Arrays.toString(payload));							
			#end
		}
		
		
		if (envelope != null) {
			#if ($infrastructure.logging == "log4j")
			logger.debug("decoded message type:"+envelope.getMessageType().name());
			#end
			switch (envelope.getMessageType()) {
				#foreach ($service in $server.services)
				case ${service.serviceName}Request : {
					try {
						final ${service.requestClassName} request = ${service.requestClassName}.parseFrom(envelope.getMessageData().toByteArray());
						#if ($infrastructure.logging == "log4j")
						logger.debug("message details:"+request.toString());
						#end
						
						#if ($service.hasResponse()) ##has a response			
							#if ($service.hasCallback()) ##has a callback
						//handle message
						${service.responseClassName} response = serviceHandler.${service.serviceName}(request, this);
						
							#else ##does not have a callback
						//handle message
						${service.responseClassName} response = serviceHandler.${service.serviceName}(request);						
							#end
												
						#if ($infrastructure.logging == "log4j")
						logger.debug("got reply: "+response.toString());
						#end
						
						//convert to generic message
						MessageContainer outgoingMessage = 
								MessageContainer.newBuilder().setMessageType(MessageType.${service.serviceName}Response).setMessageData(ByteString.copyFrom(response.toByteArray())).build();
						
						#if ($infrastructure.logging == "log4j")
						logger.debug("encoded reply:"+outgoingMessage.toString());
						#end
						
						//send
						#if ($infrastructure.logging == "log4j")
						logger.info("sending registration reply");
						#end						
						try {
							serverDataTransceiver.send(outgoingMessage.toByteArray());	
						} catch (IOException e) {
							//do nothing, just log, when the reader thread of the data transceiver will try to read data from the socket it will terminate
							#if ($infrastructure.logging == "log4j")
							logger.error("Communications Error while trying to send reply to client.",e);
							#elseif ($infrastructure.logging == "System")
							System.out.println("Error: Communications Error while trying to send reply to client."+e.getMessage());							
							#end
						}
						
						#else ##no response
						#if ($service.hasCallback()) ##has a callback
						serviceHandler.${service.serviceName}(request, this);
						#else ##does not have a callback
						serviceHandler.${service.serviceName}(request);
						#end
						
						#end
						
					} catch (InvalidProtocolBufferException e) {
						#if ($infrastructure.logging == "log4j")
						logger.error("Cannot decode data. Protocol error", e);
						logger.error("Data that cannot be decoded:"+Arrays.toString(envelope.getMessageData().toByteArray()));
						#elseif ($infrastructure.logging == "System")
						System.out.println("Error: Cannot decode data. Protocol error:"+e.getMessage());
						System.out.println("Error: Data that cannot be decoded:"+Arrays.toString(envelope.getMessageData().toByteArray()));												
						#end
					} 
					
					break;
				}
				#end
				
				default : { 
					#if ($infrastructure.logging == "log4j")
					logger.error("Received message type:"+envelope.getMessageType().name()+" that cannot be processed. Protocol error");
					#elseif ($infrastructure.logging == "System")
					System.out.println("Error: Received message type:"+envelope.getMessageType().name()+" that cannot be processed. Protocol error");			
					#end
					break;
				}
			}
		}
		

		
	}

	@Override
	public void handleCommunicationsError() {
		//received communications error trying to read data from the client.
		//the reader thread will terminate so there is nothing more to do.
		//TODO if the application wants to be notified for these events, then we should get an error handler on the constructor and report the error there
		#if ($infrastructure.logging == "log4j")
		logger.info("Received communications error.");
		#end
	}
	
	#foreach ($service in $server.services)
		#if ($service.hasCallback())
	@Override
	//sends a callback message to a client
	public void ${service.serviceName}Callback(${service.callbackClassName} callbackMessage) throws RemoteCommunicationsException {
		
		//convert to generic message
		MessageContainer outgoingMessage = 
				MessageContainer.newBuilder().setMessageType(MessageType.${service.serviceName}Callback).setMessageData(ByteString.copyFrom(callbackMessage.toByteArray())).build();
		#if ($infrastructure.logging == "log4j")
		logger.debug("encoded callback reply:"+outgoingMessage.toString());
		#end
		
		//send
		try {
			#if ($infrastructure.logging == "log4j")
			logger.info("sending callback message");
			#end
			serverDataTransceiver.send(outgoingMessage.toByteArray());							
		} catch (IOException e) {
			//do nothing, just log, when the reader thread will try to read data from the socket it will terminate
			#if ($infrastructure.logging == "log4j")
			logger.error("Communications Error while trying to send reply to client.",e);
			#end
			
			//rethrow for caller
			throw(new RemoteCommunicationsException(RemoteCommunicationsErrorType.DISCONNECTED, "Communications Error while trying to send reply to client. Info:"+e.getMessage()));
		}
	}
		#end
	#end



}
