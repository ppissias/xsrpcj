package ${server.javaPackage}.$serverSubPackage;

import ${infrastructure.javaPackage}.RemoteCommunicationsException;
import ${service.callbackType};

/**
 * This interface is used for sending asynchronous callback replies to a client
 * who called ${service.serviceName}
 * 
 * @author Petros Pissias
 *
 */
public interface $className {

	public void ${service.serviceName}Callback($callbackClassName cb) throws RemoteCommunicationsException;
}
