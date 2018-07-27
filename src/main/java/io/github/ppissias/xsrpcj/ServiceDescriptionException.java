/**
 * 
 */
package io.github.ppissias.xsrpcj;

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
