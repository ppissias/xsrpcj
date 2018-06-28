package $infrastructure.javaPackage;

/**
 * Interface implemented by users of 
 * the SocketDataTransceiver. 
 * 
 * Used in order to inform about communication error indications
 * 
 * @author Petros Pissias
 *
 */
public interface ErrorHandler {
	/**
	 * Method that indicates a communication error
	 */
	public abstract void handleCommunicationsError();
}
