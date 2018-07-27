/**
 * 
 */
package org.xsrpcj.gen;

/**
 * Simple exception for service description issues
 * @author Petros Pissias
 *
 */
public class ServiceDescriptionException extends Exception {

	public  ServiceDescriptionException(String reason) {
		super(reason);
	}
}
