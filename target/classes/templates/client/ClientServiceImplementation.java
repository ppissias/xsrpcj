package ${server.javaPackage}.$clientSubPackage;

import java.io.IOException;
import java.util.Arrays;
#if ($infrastructure.logging == "log4j")



import org.apache.log4j.Logger;
#end




import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import ${infrastructure.javaPackage}.ClientReplyHandler;
import ${infrastructure.javaPackage}.DataHandler;
import ${infrastructure.javaPackage}.RemoteCommunicationsErrorType;
import ${infrastructure.javaPackage}.RemoteCommunicationsException;
import ${infrastructure.javaPackage}.ServiceProxy;
import ${infrastructure.javaPackage}.SocketDataTransceiver;

//message type imports
#foreach( $importClass in $classImports )
import ${importClass};
#end

//generic message type import
import ${server.javaPackage}.types.${server.name}.MessageContainer;
import ${server.javaPackage}.types.${server.name}.MessageContainer.MessageType;


public class $className extends ServiceProxy implements DataHandler, ${serviceInterfaceClassName}{
	#if ($infrastructure.logging == "log4j")
	private final Logger logger = Logger.getLogger(getClass());
	#end
	
	//the reply handlers for each interaction pattern
	#foreach ($service in $server.services)
		#if ($service.hasResponse()) ##must create reply handler
	private final ClientReplyHandler<${service.responseClassName}> client${service.serviceName}ReplyHandler; 
		#end
	#end	
	
	//keep references to all service callbacks
	#foreach ($service in $server.services)
		#if ($service.hasCallback()) ##must store callback 
	private final ${server.name}${service.serviceNameUpper}ClientCallback client${service.serviceName}Callback;
		#end
	#end	
	
	//timeout waiting for a reply from the server
	private static final int timeoutSeconds = 60;
	
	public ${className}(String host, int port#foreach ($service in $server.services)#if ($service.hasCallback()), ${server.name}${service.serviceNameUpper}ClientCallback client${service.serviceName}Callback#end#end) {
		super(host, port);
		
		//create all request / reply Handlers
		#foreach ($service in $server.services)
			#if ($service.hasResponse()) ##must create reply handler
		client${service.serviceName}ReplyHandler = new ClientReplyHandler<${service.responseClassName}>(timeoutSeconds); 
			#end
		#end		
		
		//keep references to all service callbacks
		#foreach ($service in $server.services)
			#if ($service.hasCallback()) ##must store callback
		this.client${service.serviceName}Callback = client${service.serviceName}Callback;
			#end
		#end		
	}

	
	//high level access methods. All Req-Reply operations are atomic 
	//and should not overlap, thus all methods are synchronized
	#foreach( $service in $server.services )
	@Override
		#if ($service.hasResponse()) 
	public synchronized ${service.responseClassName} ${service.serviceName}(${service.requestClassName} request) throws RemoteCommunicationsException {		
	
		#else
	public synchronized void ${service.serviceName}(${service.requestClassName} request) throws RemoteCommunicationsException {		
		#end
		//create new connection of necessary
		checkDataTransceiver();

		MessageContainer outgoingMessage = 
				MessageContainer.newBuilder().setMessageType(MessageType.${service.serviceName}Request).setMessageData(ByteString.copyFrom(request.toByteArray())).build();
				
		try {
			//send
			clientDataTransceiver.send(outgoingMessage.toByteArray());
		}catch (IOException e) {
			handleSendException(e);
		}			
		
		#if ($service.hasResponse()) 
		//get reply
		${service.responseClassName} reply = client${service.serviceName}ReplyHandler.getReply();
			
		return reply;
		#end
	}
	
	#end
	

	@Override
	public void handleDataIndication(byte[] payload) {
		#if ($infrastructure.logging == "log4j")
		logger.debug("handling data indication");
		#end
		
		MessageContainer incomingMessage = null;
		try {
			incomingMessage = MessageContainer.parseFrom(payload);
		} catch (InvalidProtocolBufferException e) {
			#if ($infrastructure.logging == "log4j")
			logger.error("Cannot decode data. Protocol error", e);
			logger.error("Data that cannot be decoded:"+Arrays.toString(payload));
			#elseif ($infrastructure.logging == "System")
			System.out.println("Error: Error: Cannot decode data. Protocol error:"+e.getMessage());
			System.out.println("Error: Error: Data that cannot be decoded:"+Arrays.toString(payload));
			return;
			#end
		}
		
		if (incomingMessage != null) {
			#if ($infrastructure.logging == "log4j")
			logger.debug("decoded message type:"+incomingMessage.getMessageType().name());
			#end
			switch (incomingMessage.getMessageType()) {
				//handle normal replies
			#foreach ($service in $server.services)
				#if($service.hasResponse())
				case ${service.serviceName}Response : {
					try {
						${service.responseClassName} response = ${service.responseClassName}.parseFrom(incomingMessage.getMessageData().toByteArray());
						client${service.serviceName}ReplyHandler.insertReply(response);
					} catch (InvalidProtocolBufferException e) {
						#if ($infrastructure.logging == "log4j")
						logger.error("Cannot decode data. Protocol error", e);
						logger.error("Data that cannot be decoded:"+Arrays.toString(incomingMessage.getMessageData().toByteArray()));
						#elseif ($infrastructure.logging == "System")
						System.out.println("Error: Error: Cannot decode data. Protocol error:"+e.getMessage());
						System.out.println("Error: Error: Data that cannot be decoded:"+Arrays.toString(incomingMessage.getMessageData().toByteArray()));
						#end
						
					} catch (InterruptedException e) {
						#if ($infrastructure.logging == "log4j")
						logger.error("Interrupted while trying to insert reply to handler", e);
						#elseif ($infrastructure.logging == "System")
						System.out.println("Error: Error: Interrupted while trying to insert reply to handler");							
						#end
					}									
					break;
				}
				#end
			#end
			
				//handle callbacks
			#foreach ($service in $server.services)
				#if($service.hasCallback())
				case ${service.serviceName}Callback : {
					try {
						${service.callbackClassName} callbackMessage = ${service.callbackClassName}.parseFrom(incomingMessage.getMessageData().toByteArray());
						client${service.serviceName}Callback.${service.serviceName}Callback(callbackMessage);						
					} catch (InvalidProtocolBufferException e) {
						#if ($infrastructure.logging == "log4j")
						logger.error("Cannot decode data. Protocol error", e);
						logger.error("Data that cannot be decoded:"+Arrays.toString(incomingMessage.getMessageData().toByteArray()));
						#elseif ($infrastructure.logging == "System")
						System.out.println("Error: Error: Cannot decode data. Protocol error:"+e.getMessage());
						System.out.println("Error: Error: Data that cannot be decoded:"+Arrays.toString(incomingMessage.getMessageData().toByteArray()));						
						#end
					} 				
					break;
				}
				#end
			#end		
				
				default : { 
					#if ($infrastructure.logging == "log4j")
					logger.error("Received message type:"+incomingMessage.getMessageType().name()+" that cannot be processed. Protocol error");
					#elseif ($infrastructure.logging == "System")
					System.out.println("Error: Error: Received message type:"+incomingMessage.getMessageType().name()+" that cannot be processed. Protocol error");					
					#end
					break;
				}
			}
		}
	}

	@Override
	protected SocketDataTransceiver getDataTransceiverInstance() throws IOException {
		return new SocketDataTransceiver(host, port, this, this);
	}
	
	/**
	 * Handles an IO Exception during a send operation. 
	 * It will report the error and handling and will throw a 
	 * RemoteCommunicationsException for the caller
	 * 
	 * @param e the IOException that caused the send operation to fail
	 * @throws RemoteCommunicationsException indicating the communications problem
	 */
	private void handleSendException(IOException e) throws RemoteCommunicationsException {
		#if ($infrastructure.logging == "log4j")
		logger.error("Exception while trying to send message to server", e);		
		#end
		
		//indicate communications error
		handleCommunicationsError();
		
		//throw exception for caller
		throw (new RemoteCommunicationsException(RemoteCommunicationsErrorType.DISCONNECTED, e.getMessage()));		
	}
}
