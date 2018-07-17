/**
 * 
 */
package org.xsrpcj.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is used in order to store the information from the provided
 * services configuration on JSON format.
 * 
 * @author Petros Pissias
 *
 */
public class ServiceConfigurationReader {

	//server information
	public class Server {
		private String name;
		private int port; 
		private String javaPackage;
		
		private List<Service> services;
		
		public String getName() {
			return name;
		}
		public int getPort() {
			return port;
		}
		public List<Service> getServices() {
			return services;
		}
			
		public String getJavaPackage() {
			return javaPackage;
		}
		
		@Override
		public String toString() {
			return String.format(
					"Server [name=%s, port=%s, javaPackage=%s, services=%s]",
					name, port, javaPackage, services);
		}	
	}
	
	//service information
	public class Service {
		private String serviceName;
		private String requestType;
		private String responseType;
		private String callbackType;

		public String getServiceName() {
			return serviceName;
		}
		
		//returns the first letter uppercase
		public String getServiceNameUpper() {
			return serviceName.substring(0, 1).toUpperCase() + serviceName.substring(1);
		}
		
		public String getRequestType() {
			return requestType;
		}
		public String getResponseType() {
			return responseType;
		}
		public String getCallbackType() {
			return callbackType;
		}
		
		public String getRequestClassName() {
			if (requestType == null) {
				return null;
			} else {
				String[] splitString = requestType.split("\\.");
				return splitString[splitString.length -1];
			}
		}

		public String getResponseClassName() {
			if (responseType == null) {
				return null;
			} else {
				String[] splitString = responseType.split("\\.");
				return splitString[splitString.length -1];
			}			
		}

		public String getCallbackClassName() {
			if (callbackType == null) {
				return null;
			} else {
				String[] splitString = callbackType.split("\\.");
				return splitString[splitString.length -1];
			}		
		}
		
		public boolean hasResponse() {
			if (responseType == null) {
				return false;
			} else {
				return true;
			}
		}
		
		public boolean hasCallback() {
			if (callbackType == null) {
				return false;
			} else {
				return true;
			}			
		}
		
		@Override
		public String toString() {
			return String
					.format("Service [serviceMame=%s, requestType=%s, responseType=%s, callbackType=%s]",
							serviceName, requestType, responseType,
							callbackType);
		}				
	}
	
	//infrastructure section
	public class Infrastructure {
		private String javaPackage;
		private String logging;
		@Override
		public String toString() {
			return String.format("Infrastructure [javaPackage=%s, logging=%s]",
					javaPackage, logging);
		}
		public String getJavaPackage() {
			return javaPackage;
		}
		public String getLogging() {
			return logging;
		}					
	}
	
	//overall service description class
	public class ServiceDescription {
		private List<Server> servers;
		private Infrastructure infrastructure;
		
		public List<Server> getServers() {
			return servers;
		}

		public Infrastructure getInfrastructure() {
			return infrastructure;
		}

		@Override
		public String toString() {
			return String
					.format("ServiceDescription [servers=%s, infrastructure=%s]",
							servers, infrastructure);
		}			
	}	
	
	

	/**
	 * Returns an instance of a Service configuration object, containing all the necessary information
	 * for generating the relevant classes
	 * 
	 * @param relativePath the relative path of the JSON file
	 * @return an instance of a Service configuration object
	 * @throws IOException in case the JSON file cannot be found
	 */
	public static ServiceDescription parseSeviceConfigurationFile(String relativePath) throws IOException {
		//create Gson parser
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		//get file as a string
		String fileData = new String(Files.readAllBytes(Paths.get(relativePath)));
		//parse file with Gson
		ServiceDescription parsedJson = gson.fromJson(fileData, ServiceDescription.class);
				
		return parsedJson;
	}

}
