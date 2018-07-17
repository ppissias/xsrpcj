package org.xsrpcj.gen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xsrpcj.gen.ServiceConfigurationReader.Server;
import org.xsrpcj.gen.ServiceConfigurationReader.Service;
import org.xsrpcj.gen.ServiceConfigurationReader.ServiceDescription;

/**
 * This is the entry point for starting the file generator
 * 
 * It will parse the JSON file containing the information for all servers and services
 * and it will generate the relevant classes and .proto files.
 * 
 * @author Petros Pissias
 *
 */
public class XsRPCJGenerator {

	//logger
	private final Logger logger;
	
	//velocity engine & context
	private final VelocityEngine ve = new VelocityEngine();	
	private final VelocityContext velocityContext = new VelocityContext();
	
	//sub-packages
	public static final String serverSideSubPackage = "server";
	public static final String clientSideSubPackage = "client";
	public static final String serverSideImplSubPackage = "serverimpl";
	
	//template paths
	public static final String infrastructureTemplatesPath = "templates/infrastructure";
	public static final String serverSideTemplatesPath = "templates/server";
	public static final String clientSideTemplatesPath = "templates/client";
	public static final String protoTemplatePath = "templates/proto";
	
	//server side templates
	public static final String serverServerTemplateFilename = "Server.java";
	public static final String serverCallbackInterfaceTemplateFilename = "ServerCallbackInterface.java";
	public static final String serverServiceInterfaceTemplateFilename = "ServerServiceInterface.java";
	public static final String serverClientHandlerTemplateFilename = "ClientHandler.java";
	
	//server side impl templates
	public static final String serverServiceInterfaceImplTemplateFilename = "ServerServiceInterfaceImpl.java";
	
	//client side templates
	public static final String clientCallbackInterfaceTemplateFilename = "ClientCallbackInterface.java";
	public static final String clientServiceInterfaceTemplateFilename = "ClientServiceInterface.java";
	public static final String clientServiceImplTemplateFilename = "ClientServiceImplementation.java";
	
	//proto template
	public static final String protoTemplateFilename = "MessageContainer.proto";
	
	//simple counter
	public class generatorCounter {
		private int count = -1;
		
		public int getNext() {
			count++;
			return count;
		}
	}
	
	public XsRPCJGenerator() {
		logger = LoggerFactory.getLogger(getClass()); 
		
		//set velocity resource loader
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		//init velocity engine
		ve.init();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	
		Logger logger = LoggerFactory.getLogger(XsRPCJGenerator.class);

		String cmd_usage = "expected arguments: [-client] [-server] [-infrastructure] <source generation path> <service-description-file> ";
		if (args.length < 3 || args.length > 5) {
			System.out.println(cmd_usage);
			return;
		}
		
		boolean generateClientFiles = false; 
		boolean generateServerFiles = false;
		boolean generateInfrastructureFiles = false;
		
		String protocPath = System.getenv("PROTOC_PATH");
		
		for (String arg : args) {
			if (arg.equals("-client")) {
				generateClientFiles = true;
			} else if (arg.equals("-server")) {
				generateServerFiles = true;
			} else if (arg.equals("-infrastructure")) {
				generateInfrastructureFiles = true;
			} 	
		}
		
		if ( (generateClientFiles | generateServerFiles | generateInfrastructureFiles) == false ) {
			//all false
			System.out.println(cmd_usage);
			logger.info("at least one of the options -client, -server, -infrastructure must be specified");
			return;
		}
		//last argument is the file name
		String jsonFile = args[args.length-1];
		//the one before is the source generation path
		String sourcePath = args[args.length-2];
		
		new XsRPCJGenerator().generateSourceFiles(generateClientFiles, generateServerFiles, generateInfrastructureFiles, jsonFile, sourcePath, protocPath);
	}

	
	/**
	 * Provide programmatic access to the generator 
	 * @param generateClientFiles boolean flag indicating if we want to generate the client side files
	 * @param generateServerFiles boolean flag indicating if we want to generate the server side files
	 * @param generateInfrastructureFiles boolean flag indicating if we want to generate the infrastructure files
	 * @param jsonFile the full path to the JSON service description
	 * @param sourcePath the full path to the directory where the java source files and the proto file will be generated
	 * @param protocPath path to the protocol buffers compiler executable. If present the process will also automatically compile the generated .proto files
	 * @throws Exception 
	 */
	public synchronized static void generate(boolean generateClientFiles, boolean generateServerFiles,
			boolean generateInfrastructureFiles, String jsonFile, String sourcePath, String protocPath) throws Exception{
		
		new XsRPCJGenerator().generateSourceFiles(generateClientFiles, generateServerFiles, generateInfrastructureFiles, jsonFile, sourcePath, protocPath);
	}
	
	/**
	 * Top level method for the generation of all files
	 * @param generateClientFiles if the generator should generate the client side files
	 * @param generateServerFiles if the generator should generate the server side files
	 * @param generateInfrastructureFiles if the generator should generate the infrastructure files
	 * @param jsonFile the source json file containing the all service descriptions
	 * @param sourcePath the source path where the files will be generated
	 * @param protocPath the path to the protocol buffers compiler executable
	 * @throws Exception  
	 */
	private void generateSourceFiles(boolean generateClientFiles,
			boolean generateServerFiles, boolean generateInfrastructureFiles,
			String jsonFile, String sourcePath, String protocPath) throws Exception {

		//decode input
		ServiceDescription servicesDesc = null;
		try {
			servicesDesc = ServiceConfigurationReader.parseSeviceConfigurationFile(jsonFile);
			
			//TODO
			//do sanity check
		} catch (IOException e) {
			logger.error("Cannot read json file:"+jsonFile);
			return;
		}


		
		//set initial context
		velocityContext.put("infrastructure", servicesDesc.getInfrastructure());
		
		//sub package names
		velocityContext.put("serverSubPackage", serverSideSubPackage);
		velocityContext.put("serverImplSubPackage", serverSideImplSubPackage);
		velocityContext.put("clientSubPackage", clientSideSubPackage);
		
		if (generateInfrastructureFiles) {		
			generateInfrastructureFiles(servicesDesc.getInfrastructure().getJavaPackage(), sourcePath);
		}
		
		for (Server server : servicesDesc.getServers()) {
			logger.info("Processing Server:"+server.getName());
			
			if (generateServerFiles) {
				generateServerFiles(server, sourcePath);
			}
			
			if (generateClientFiles) {
				generateClientFiles(server, sourcePath);
			}
			
			generateProtoFile(server, sourcePath);
		}

		String protoSourcePath =sourcePath+"/../proto"; 
		
		if (protocPath == null) {
			
			for (Server server : servicesDesc.getServers()) {			
				//output message
				logger.info("Files generated. Please compile the generated .proto file : "+server.getName()+"MessageContainer.proto");
				logger.info("with a command like: \nprotoc -I"+protoSourcePath+" --java_out="+sourcePath+" "+protoSourcePath+"/"+server.getName()+"MessageContainer.proto");
			}
		} else {
			for (Server server : servicesDesc.getServers()) {		
				//compile the generated .proto files
				String protoSourceFile = protoSourcePath+"/"+server.getName()+"MessageContainer.proto";
				logger.info("compiling "+protoSourceFile);
				ProcessBuilder pb = new ProcessBuilder(protocPath, "-I"+protoSourcePath, "--java_out="+sourcePath, protoSourceFile);
				Process p = pb.start();
				int retCode = p.waitFor();
				if (retCode != 0) {
					logger.info("could not compile "+protoSourceFile);
					logger.info("please compile manually with a command like: \nprotoc -I"+protoSourcePath+" --java_out="+sourcePath+" "+protoSourceFile);
				} else {
					logger.info("compiled "+protoSourceFile);
				}
			}
		}
		
	}


	/**
	 * Generates the infrastructure files
	 * @param sourcePath 
	 * @param package 
	 * @throws IOException 
	 */
	private void generateInfrastructureFiles(String javaPackage, String sourcePath) throws IOException {
		
		logger.info("Generating infrastructure files");

		//replace package only & logging....
		String[] infrastructureFileNames = new String[] {"ClientReplyHandler.java", 
				"DataHandler.java", "ErrorHandler.java", "RemoteCommunicationsException.java","RemoteCommunicationsErrorType.java", 
				"ServiceProxy.java", "SocketDataTransceiver.java", "SocketDataTransceiverReaderThread.java"};
		
		
		for (String infrastructureFile : infrastructureFileNames) {
			String outputFile = sourcePath + "/" + getFilePath(javaPackage, infrastructureFile);
			logger.info("Will write file:"+outputFile);
			
			generateSourceFile(outputFile, infrastructureTemplatesPath+"/"+infrastructureFile);
		}
		
	}
	
	
	/**
	 * Generates the client side files for the provided Server description
	 * @param server the Server object containing all relevant information
	 * @param sourcePath the source file, where to generate the output files
	 * @throws IOException 
	 */
	private void generateClientFiles(Server server, String sourcePath) throws IOException {
		logger.info("Generating client side files for server:"+server.getName());

		//add context	
		velocityContext.put("server", server);
		
		//generate client side callback interfaces
		List<String> cbInterfaceNames = generateClientCallbackInterfaces(server, sourcePath);
		
		//generate client service interface
		String serviceInterfaceClassName = generateClientServiceInterface(server, sourcePath);

		//generate client service implementation
		String serviceImplementationClassName = generateClientServiceImplementation(server, sourcePath, cbInterfaceNames, serviceInterfaceClassName);
		
		
		velocityContext.remove("server");
	}

	/**
	 * Generates the client side callback interface files
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @return the interface names that were generated
	 * @throws IOException
	 */
	private List<String> generateClientCallbackInterfaces(Server server, String sourcePath) throws IOException {
		List<String> cbInterfaceNames = new ArrayList<String>();
		//generate all callback interfaces
		for (Service service : server.getServices()) {
			if (service.getCallbackType() != null) {
				
				String clientCallbackTemplatePath = clientSideTemplatesPath+"/"+clientCallbackInterfaceTemplateFilename;
				String className = server.getName()+service.getServiceNameUpper()+"ClientCallback";
				String fileName = className+".java";
				String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+clientSideSubPackage, fileName);
				
				//insert into context
				velocityContext.put("service", service);
				velocityContext.put("className", className);
				velocityContext.put("callbackClassName", getClassName(service.getCallbackType()));
				cbInterfaceNames.add(className);
				//generate
				generateSourceFile(filePath, clientCallbackTemplatePath);
				//remove from context
				velocityContext.remove("className");
				velocityContext.remove("service");
				velocityContext.remove("callbackClassName");
			}
		}
		
		return cbInterfaceNames;
	}
	
	/**
	 * Generates the Server service interface source file
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @return the name of the generated class
	 * @throws IOException
	 */
    private String generateClientServiceInterface(Server server, String sourcePath) throws IOException {
		//generate Server source file
		String serviceInterfaceTemplatePath = clientSideTemplatesPath+"/"+clientServiceInterfaceTemplateFilename;
		String className = server.getName()+"ClientService";
		String fileName = className+".java";
		String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+clientSideSubPackage, fileName);
		
		List<String> classImports = new ArrayList<String>();
		for (Service service: server.getServices()) {
			if (service.getRequestType() != null && !classImports.contains(service.getRequestType())) {
				classImports.add(service.getRequestType());
			}
			
			if (service.getResponseType() != null && !classImports.contains(service.getResponseType())) {
				classImports.add(service.getResponseType());
			}
			
			if (service.getCallbackType() != null && !classImports.contains(service.getCallbackType())) {
				classImports.add(service.getCallbackType());
			}	
		}
				
		//insert into context
		velocityContext.put("className", className);
		velocityContext.put("classImports", classImports);
		//generate
		generateSourceFile(filePath, serviceInterfaceTemplatePath);
		//remove from context
		velocityContext.remove("className");
		velocityContext.remove("classImports");
		return className;    	
    }	
	
    
	/**
	 * Generates the client side implementation of the service
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @param serviceInterfaceClassName the service interfaces
	 * @return the name of the generated class
	 * @throws IOException
	 */
    private String generateClientServiceImplementation(Server server, String sourcePath, List<String> callbackInterfaces, String serviceInterfaceClassName) throws IOException {
		//generate Server source file
		String clientServiceImplementationTemplatePath = clientSideTemplatesPath+"/"+clientServiceImplTemplateFilename;
		String className = server.getName()+"ClientServiceImpl";
		String fileName = className+".java";
		String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+clientSideSubPackage, fileName);
		
		List<String> classImports = new ArrayList<String>();
		for (Service service: server.getServices()) {
			if (service.getRequestType() != null && !classImports.contains(service.getRequestType())) {
				classImports.add(service.getRequestType());
			}
			
			if (service.getResponseType() != null && !classImports.contains(service.getResponseType())) {
				classImports.add(service.getResponseType());
			}
			
			if (service.getCallbackType() != null && !classImports.contains(service.getCallbackType())) {
				classImports.add(service.getCallbackType());
			}	
		}
				
		//insert into context
		velocityContext.put("className", className);
		velocityContext.put("classImports", classImports);
		velocityContext.put("callbackInterfaces", callbackInterfaces);
		velocityContext.put("serviceInterfaceClassName", serviceInterfaceClassName);
		//generate
		generateSourceFile(filePath, clientServiceImplementationTemplatePath);
		//remove from context
		velocityContext.remove("className");
		velocityContext.remove("classImports");
		velocityContext.remove("callbackInterfaces");
		velocityContext.remove("serviceInterfaceClassName");
		return className;    	
    }
    
    
	/**
	 * Generates the server side files for the provided Server description
	 * @param server the Server object containing all relevant information
	 * @param sourcePath the source file, where to generate the output files
	 * @throws IOException 
	 */	
	private void generateServerFiles(Server server, String sourcePath) throws IOException {		
		logger.info("Generating server side files for server:"+server.getName());
		
		//add context	
		velocityContext.put("server", server);
		
		//generate server side callback interfaces 
		List<String> cbInterfaceNames = generateServerCallbackInterfaces(server, sourcePath);

		//generate server service interface 
		String serviceInterfaceClassName = generateServerServiceInterface(server, sourcePath);
		
		//generate Client handler
		String clientHandlerClassName = generateServerClientHandler(server, sourcePath, cbInterfaceNames, serviceInterfaceClassName);

		//generate the server server source file
		String serverClassName = generateServerServer(server, sourcePath, serviceInterfaceClassName, clientHandlerClassName);
		velocityContext.remove("server");
	}
	

	/**
	 * Generates the server side callback interface files
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @return the interface names that were generated
	 * @throws IOException
	 */
	private List<String> generateServerCallbackInterfaces(Server server, String sourcePath) throws IOException {
		List<String> cbInterfaceNames = new ArrayList<String>();
		//generate all callback interfaces
		for (Service service : server.getServices()) {			
			if (service != null && service.getCallbackType() != null) {
				
				String serverCallbackTemplatePath = serverSideTemplatesPath+"/"+serverCallbackInterfaceTemplateFilename;
				String className = server.getName()+service.getServiceNameUpper()+"ServerCallback";
				String fileName = className+".java";
				String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+serverSideSubPackage, fileName);
				
				//insert into context
				velocityContext.put("service", service);
				velocityContext.put("className", className);
				velocityContext.put("callbackClassName", getClassName(service.getCallbackType()));
				cbInterfaceNames.add(className);
				//generate
				generateSourceFile(filePath, serverCallbackTemplatePath);
				//remove from context
				velocityContext.remove("className");
				velocityContext.remove("service");
				velocityContext.remove("callbackClassName");
			}
		}
		
		return cbInterfaceNames;
	}
	
	/**
	 * Generates the Server service interface source file
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @return the name of the generated class
	 * @throws IOException
	 */
    private String generateServerServiceInterface(Server server, String sourcePath) throws IOException {
		//generate Server source file
		String serviceInterfaceObjectTemplatePath = serverSideTemplatesPath+"/"+serverServiceInterfaceTemplateFilename;
		String className = server.getName()+"ServerService";
		String fileName = className+".java";
		String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+serverSideSubPackage, fileName);
		
		List<String> classImports = new ArrayList<String>();
		for (Service service: server.getServices()) {
			if (service.getRequestType() != null && !classImports.contains(service.getRequestType())) {
				classImports.add(service.getRequestType());
			}
			
			if (service.getResponseType() != null && !classImports.contains(service.getResponseType())) {
				classImports.add(service.getResponseType());
			}
			
			if (service.getCallbackType() != null && !classImports.contains(service.getCallbackType())) {
				classImports.add(service.getCallbackType());
			}	
		}
				
		//insert into context
		velocityContext.put("className", className);
		velocityContext.put("classImports", classImports);
		//generate
		generateSourceFile(filePath, serviceInterfaceObjectTemplatePath);
		//remove from context
		velocityContext.remove("className");
		velocityContext.remove("classImports");
		return className;    	
    }
	
	/**
	 * Generates the Server client handler source file
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @param serviceInterfaceClassName 
	 * @return the name of the generated class
	 * @throws IOException
	 */
    private String generateServerClientHandler(Server server, String sourcePath, List<String> callbackInterfaces, String serviceInterfaceClassName) throws IOException {
		//generate Server source file
		String serverClienthandlerTemplatePath = serverSideTemplatesPath+"/"+serverClientHandlerTemplateFilename;
		String className = server.getName()+"ClientHandler";
		String fileName = className+".java";
		String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+serverSideSubPackage, fileName);
		
		List<String> classImports = new ArrayList<String>();
		for (Service service: server.getServices()) {
			if (service.getRequestType() != null && !classImports.contains(service.getRequestType())) {
				classImports.add(service.getRequestType());
			}
			
			if (service.getResponseType() != null && !classImports.contains(service.getResponseType())) {
				classImports.add(service.getResponseType());
			}
			
			if (service.getCallbackType() != null && !classImports.contains(service.getCallbackType())) {
				classImports.add(service.getCallbackType());
			}	
		}
				
		//insert into context
		velocityContext.put("className", className);
		velocityContext.put("classImports", classImports);
		velocityContext.put("callbackInterfaces", callbackInterfaces);
		velocityContext.put("serviceInterfaceClassName", serviceInterfaceClassName);
		//generate
		generateSourceFile(filePath, serverClienthandlerTemplatePath);
		//remove from context
		velocityContext.remove("className");
		velocityContext.remove("classImports");
		velocityContext.remove("callbackInterfaces");
		velocityContext.remove("serviceInterfaceClassName");
		return className;    	
    }
    
	/**
	 * Generates the Server Server source file
	 * @param server the server object
	 * @param sourcePath the output source path
	 * @param clientHandlerClassName 
	 * @param serviceInterfaceClassName 
	 * @return the name of the generated class
	 * @throws IOException
	 */
	private String generateServerServer(Server server, String sourcePath, String serviceInterfaceClassName, String clientHandlerClassName) throws IOException {
		//generate Server source file
		String serverObjectTemplatePath = serverSideTemplatesPath+"/"+serverServerTemplateFilename;
		String className = server.getName()+"Server";
		String fileName = className+".java";
		String filePath = sourcePath + "/" + getFilePath(server.getJavaPackage()+"."+serverSideSubPackage, fileName);
		
		//insert into context
		velocityContext.put("className", className);
		velocityContext.put("serviceInterfaceClassName", serviceInterfaceClassName);
		velocityContext.put("clientHandlerClassName", clientHandlerClassName);
		
		//generate
		generateSourceFile(filePath, serverObjectTemplatePath);
		//remove from context
		velocityContext.remove("className");
		velocityContext.remove("serviceInterfaceClassName");
		velocityContext.remove("clientHandlerClassName");
		
		return className;
	}
	
	
	/**
	 * Generates the Proto file containing the top level definitions for the particular server
	 * @param server the Server object containing all relevant information
	 * @param proto the Proto object containing the relevant information 
	 * @param sourcePath the source path, where to generate the file
	 * @throws IOException 
	 */
	private void generateProtoFile(Server server, String sourcePath) throws IOException {
		logger.info("Generating Proto file for server:"+server.getName());
		
		//generate the generic message container proto file
		String protoTemplateFullPath = protoTemplatePath+"/"+protoTemplateFilename;
		String protoName = server.getName()+"MessageContainer";
		String fileName = protoName+".proto";
		String filePath = sourcePath + "/../proto/"+fileName;
		
		//insert into context
		//add context	
		velocityContext.put("server", server);
		velocityContext.put("protoName", protoName);
		velocityContext.put("counter", new generatorCounter());
		//generate
		generateSourceFile(filePath, protoTemplateFullPath);
		//remove from context
		velocityContext.remove("protoName");	
		velocityContext.remove("counter");
		velocityContext.remove("server");
	}
	

	/**
	 * Generates a source file accoprding to the template and the global current context
	 * @param filePath the source file path
	 * @param templatePath the template to be used path
	 * @throws IOException 
	 */
	private void generateSourceFile(String filePath, String templatePath) throws IOException {
		Template template = ve.getTemplate(templatePath);
		
		File outputFile = new File(filePath);
		outputFile.getParentFile().mkdirs();
		
		StringWriter stringWriter = new StringWriter();
		template.merge(velocityContext, stringWriter);
		 		 
		logger.info("Wrote file:"+filePath);
		//logger.info("content:\n"+stringWriter);
		
		FileWriter fileWriter = new FileWriter(outputFile);
		fileWriter.write(stringWriter.toString());
		fileWriter.flush();
		fileWriter.close();
	}
	
	/**
	 * REturns the file path according to the package
	 * @param javaPackage
	 * @param fileName
	 * @return
	 */
	private String getFilePath(String javaPackage, String fileName) {
		String filePath = javaPackage.replaceAll("\\.", "/");
		
		return filePath+"/"+fileName;
		
	}
	
	/**
	 * Returns the class name from the full class package
	 * @param fullClassPackage the full class package, i.e. org.pp.ClassName
	 * @return the class name, i.e. ClassName
	 */
	private String getClassName(String fullClassPackage) {
		String[] classSplit = fullClassPackage.split("\\.");
		return classSplit[classSplit.length-1];
	}
	


}
