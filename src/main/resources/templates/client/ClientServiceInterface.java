package ${server.javaPackage}.$clientSubPackage;

import ${infrastructure.javaPackage}.RemoteCommunicationsException;
#foreach( $importClass in $classImports )
import ${importClass};
#end

/**
 * Service interface on the client-side.
 * all methods throw a @link{RemoteCommunicationsException} in case of commmunication problems
 * @author Petros Pissias
 *
 */
public interface $className {

	#foreach( $service in $server.services )
		#if ($service.hasResponse()) 
	public ${service.responseClassName} ${service.serviceName}(${service.requestClassName} request) throws RemoteCommunicationsException;		
		
		#else
	public void ${service.serviceName}(${service.requestClassName} request) throws RemoteCommunicationsException;		
	
		#end
	#end
}

