package $infrastructure.javaPackage;

/**
 * Interface implemented by users of 
 * the SocketDataTransceiver. 
 * 
 * Used in order to inform about data indications
 * 
 * @author Petros Pissias
 *
 */
public interface DataHandler {
	/**
	 * Method that indicates that a new complete packet has arrived 
	 * @param payload the packet data (payload)
	 */
	public abstract void handleDataIndication(byte[] payload);
}
