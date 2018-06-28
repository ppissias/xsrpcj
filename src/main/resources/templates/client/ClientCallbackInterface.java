package ${server.javaPackage}.$clientSubPackage;

import ${service.callbackType};

/**
 * This interface is used for receiving asynchronous callback replies for service ${service.serviceName}
 * 
 * @author Petros Pissias
 *
 */
public interface $className {

	public void ${service.serviceName}Callback($callbackClassName cb);
}
