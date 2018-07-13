package ${server.javaPackage}.$serverSubPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

#if ($infrastructure.logging == "log4j")
import org.apache.log4j.Logger;
#end

/**
 * @author Petros Pissias
 *
 */
public class ${className} extends Thread {
	
	#if ($infrastructure.logging == "log4j")
	private final Logger logger = Logger.getLogger(getClass());
	#end
	
	private final $serviceInterfaceClassName serviceHandler;
			
	private final int port;	
	private final int defaultport = $server.port; 
			
	public ${className}($serviceInterfaceClassName serviceHandler) {
		port = defaultport;
		this.serviceHandler = serviceHandler;				
	}
	
	public ${className}($serviceInterfaceClassName serviceHandler, int port) {
		this.port = port;
		this.serviceHandler = serviceHandler;				
	}
	
	public void run() {
		
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			#if ($infrastructure.logging == "log4j")
			logger.error("Listening for connections on port:"+port);
			#elseif ($infrastructure.logging == "System")
			System.out.println("Listening for connections on port:"+port);		
			#end
			
			while (true) {
				#if ($infrastructure.logging == "log4j")
				logger.info("Waiting for connection...");
				#end
				final Socket incomingConnection = serverSocket.accept();
				#if ($infrastructure.logging == "log4j")
				logger.info("incoming connection arrived");
				#end
				
				try {
					//handle client 
					$clientHandlerClassName handler = new ${clientHandlerClassName}(incomingConnection,serviceHandler);
					
					handler.initialize();
					#if ($infrastructure.logging == "log4j")
					logger.info("created client handler");
					#end
				} catch (IOException ioex) {
					#if ($infrastructure.logging == "log4j")
					logger.error("Problem with client connection originating from:"+incomingConnection.getInetAddress());
					#elseif ($infrastructure.logging == "System")
					System.out.println("Error: Problem with client connection originating from:"+incomingConnection.getInetAddress());
					#end
				}
			}
		} catch (IOException ioex) {
			#if ($infrastructure.logging == "log4j")
			logger.error("Cannot accept connection on port:"+port);
			#elseif ($infrastructure.logging == "System")
			System.out.println("Error: Cannot accept connection on port:"+port);		
			#end
			return;
		} 
	}
}
