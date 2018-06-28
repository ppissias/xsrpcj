package $infrastructure.javaPackage;


/**
 * @author Petros Pissias
 *
 */
public class RemoteCommunicationsException extends Exception {

	//the exception error type
	private final RemoteCommunicationsErrorType errorType;
	
	//an optional reason 
	private final String optionalReason;
	
	/**
	 * Creates a new instance of this exception
	 * @param errorType the exception error type
	 * @param optionalInfo an optional reason or more information
	 */
	public RemoteCommunicationsException(RemoteCommunicationsErrorType errorType, String optionalInfo) {
		super("Remote communications exception. Type:"+errorType.name()+(optionalInfo==null?"":" additional info:"+optionalInfo));
		this.errorType = errorType;
		this.optionalReason = optionalInfo;
	}

	public RemoteCommunicationsErrorType getErrorType() {
		return errorType;
	}

	public String getOptionalReason() {
		return optionalReason;
	}

	@Override
	public String toString() {
		return String
				.format("RemoteCommunicationsException [errorType=%s, optionalReason=%s, toString()=%s]",
						errorType, optionalReason, super.toString());
	}
	

}
