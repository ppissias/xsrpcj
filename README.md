

# xsrpcj

  

**xsrpcj** is an extra small (xs) remote procedure call (rpc) implementation for java on top of TCP Sockets, It is designed to be used with **protocol buffers** generated data types but it can also use custom data types. It supports 3 interaction patterns:

  

- **Oneway**: similar to a void function. We just send some data.

- **Request/Reply**: we send some data and get some data back as a reply

- **Request/Reply/Callback**: in addition to the request/reply interaction pattern, we also get asynchronously a number of callback messages.

  

A typical example of the request/reply/callback interaction pattern is that we make an interaction (for example a registration) and then as a result of this interaction we get back asynchronously data from time to time.

  

xsrpcj focuses on a **simple programming model** and **minimal dependencies**. In fact, you don't need anything else except protocol buffers, not even xsrpcj itself, since it generates all the code you need.

  

## Getting Started

*A good way to start is to check out the xsrpcj examples project (https://github.com/ppissias/xsrpcj-examples) -also check the wiki*  

OK, so you have in mind some **services** you want to **implement**. You need to **express** these services in terms of **interaction patterns** and create the **data types** for the various requests replies and possible callbacks. A **typical workflow** using xsrpcj is:

  

- You **describe your services** (this is done in a **JSON** file)

- You **create** your **data types** (either using **protocol buffers**, or using your own custom data types - as long as they obey some simple rules, explained later)

- You invoke the **xsrpcj code generator**

**That's it.** xsrpcj will generate a client API and and all the necessary server-side code.

  

Lets take it **step by step**.



  

### Service description

Services are described in a JSON file. A simple example is provided below:

    {
	"servers":[
	 	{
			"name": "Persons",
			"port": "22100",
			"javaPackage":"io.github.ppissias.xsrpcj.example.simple",
			"services":[
				{"serviceName":"search", "requestType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonRequest", "responseType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" },
				{"serviceName":"notify", "requestType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.PersonNotificationRequest", "responseType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.PersonNotificationResponse" , "callbackType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" }
			]
		}		
	]
	} 




Services are grouped into Servers. Each server implements a set of services and listens on a specific TCP port.
In the example above, we define 2 services realized by a server named "Persons". 

Lets look closer at the definition file, each **server** has : 

- a **name**, **port** and **javaPackage**: the name is used to identify the server (this will be used for the generation of the name of the method in both the client and server APIs, in combination with the service name) and the port will be the default listen port. The javaPackage is used to define the base package for the generated code.

and each **service** definition has :

 - a **serviceName**: Used to identify the service 

 - a **request type**: This is the data type that is sent to the server when invoking the service (the request data)

 - an **optional response type**: This is the data type we get back as a reply when invoking the service

 - an **optional callback type**: This is the data type we get back asynchronously from time to time as a result of invoking the service

  
### Code generation

after invoking the xsrpcj code generator for a service description file, we will get a server-side API and a client-side API. 


**Client Side API**

For the client-side we will get an interface & its implementation for each Server, which is all we need to start invoking services.

For the example above we will get  a client side interface:

    public interface PersonsClientService {

		public SearchPersonResponse search(SearchPersonRequest request) throws RemoteCommunicationsException;		
		
		public PersonNotificationResponse notify(PersonNotificationRequest request) throws RemoteCommunicationsException;
	}		
		

		


and its implementation (which implements the low level RPC and encoding stuff).  


The way to start invoking services is :

 
		//get a service reference
		PersonsClientService serverRef = new PersonsClientServiceImpl(serverHost, cbHandler);			

		
		//now we can use it to make calls on the server
		
		//request / response call
		SearchPersonResponse resp = serverRef.search(....);
		
		//request / response / callback call
		PersonNotificationResponse notResp = serverRef.notify(....);

					
Notice that we need to pass a "callback handler" (`cbHandler`) when we instantiate `ExampleClientServiceImpl`. We need to do this for each service that implements a callback, as we need to provide a handler for the asynchronous callback messages. 

In our particular case, we only have 1 service with a callback, so we need to provide just 1 callback handler. 

The callback handler needs to implement an interface which defines a method able to process the callback  messages. In our particular example it looks like:

    public interface PersonsNotifyClientCallback {
		public void notifyCallback(SearchPersonResponse cb);
	}


The naming convention comes from the Server and Service name descriptions, which can be as small and simple as you like, or long if you like detailed names.     

Besides the callback, we pass an additional argument, `serverHost` which is the host where the service is implemented (we can also pass a port argument, if the server listens on a different port than the default)

    ExampleClientService serverRefDefaultPort = new ExampleClientServiceImpl("localhost", cbHandler);
    	
Even though the service description already has a predefined port, on the client side we have the flexibility to override this setting.:

    ExampleClientService serverRef = new ExampleClientServiceImpl("localhost", 22100, cbHandler);
    

**Server side API**
  
On the server side, we of course need to start the server. 

		PersonsServerService serviceImplementation = new PersonsServerService() {
			//interface implementation
			//here you implement the actual service
		};
		
		//start on default port
		new PersonsServer(serviceImplementation).start();

		//start on other port than the pre-defined
		//new PersonsServer(serviceImplementation, 22123).start();

We can use the default port, or use a custom port by invoking another constructor. Everything is automatically generated except the `serviceImplementation`, which is the actual implementation of your service. 

Lets look at the generated interface that you need to implement. 

    public interface PersonsServerService {

		public SearchPersonResponse search(SearchPersonRequest request);		
		
		public PersonNotificationResponse notify(PersonNotificationRequest request, PersonsNotifyServerCallback callback);	
	}
		

		
It is the kind of interface that you would expect, according to the service description. Notice that you receive a callback object on method `notify` that you can use in order to send asynchronously responses to the caller. The underlying implementation is automatically generated and will route back the reply to the client callback handler on the client side. 

That's it ! 

You can see the full example source code here for the 

 - .proto message definition file : [SearchMessages.proto](https://github.com/ppissias/xsrpcj-examples/blob/master/xsrpcj-simple/src/main/proto/SearchMessages.proto)
  - service description file : [service-desc.json](https://github.com/ppissias/xsrpcj-examples/blob/master/xsrpcj-simple/src/main/proto/service-desc.json)
 - Client Implementation : [SearchExampleServer.java](https://github.com/ppissias/xsrpcj-examples/blob/master/xsrpcj-simple/src/main/java/SearchExampleServer.java) 
 - Server Implementation : [SearchExampleClient.java](https://github.com/ppissias/xsrpcj-examples/blob/master/xsrpcj-simple/src/main/java/SearchExampleClient.java)

  
## Examples

Download the xsrpcj examples project (https://github.com/ppissias/xsrpcj-examples) which contains several examples on using xsrpcj.


## Compiling

 
after downloading the repository, go to the home directory and type

    mvn clean compile assembly:single

This command will compile everything and assemble it as a single executable jar (named xsrpcj-1.0.0-jar-with-dependencies.jar). You can then use the produced .jar file (see section below) in order to generate your RPC code.  You can also use it through ant and maven (see sections below).
If you plan to use the generator via Maven, then optionally you can compile it for your local repository

    mvn clean install

## Using the generator 
The generator can be used in 2 ways: 

 - by running the produced .jar and providing the required arguments
	 - maven, ant & gradle integration is easy (see next chapters)
 - programatically by invoking a static method
 

Lets see all of them in detail: 

### running the produced .jar manually
First of all you need to define an environment variable named `PROTOC_PATH` pointing to the protoc compiler executable full path  (i.e. `PROTOC_PATH = /path/to/protoc` )

Then you can invoke the code generator


    java -jar xsrpcj-1.0.0-jar-with-dependencies.jar
    
    expected arguments: [-client] [-server] [-infrastructure] <source generation path> <service-description-file>

**No matter how you invoke the generator (manually, programatically, maven, ant, ...) the following example usage applies. There are examples on how to invoke the generator programmatically and via ant / maven in the next sections.**
    
### Example Usage
for example assuming that you have the following service description file in `proto/service-description.json`

    {
	"servers":[
	 	{
			"name": "Persons",
			"port": "22100",
			"javaPackage":"io.github.ppissias.xsrpcj.example.simple",
			"services":[
				{"serviceName":"search", "requestType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonRequest", "responseType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" },
				{"serviceName":"notify", "requestType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.PersonNotificationRequest", "responseType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.PersonNotificationResponse" , "callbackType":"io.github.ppissias.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" }
			]
		}		
	], 
	"infrastructure" : {
		"javaPackage":"io.github.ppissias.xsrpcj.example.simple.comms",
		"logging":"System"
	}
	}	 



when you invoke the code generator with the following arguments: 

        java -jar xsrpcj-1.0.0-jar-with-dependencies.jar -server -client -infrastructure src proto/service-desc.json 


It will generate the required code for the server, client and the common infrastructure files (used for the low level communication). The infrastructure files (`infrastructure` section in the JSON file) are needed only once, so if you generate multiple services on the same application you do not need to generate them again and again. 

It will generate all source code under the `src` directory according to the `proto/service-desc.json` service description.

**Generated files in detail**

Before the generation we started with the following file structure

    
    src
	    main
	        java
	        |   |    SearchExampleClient.java 	- client logic (using the generated code)
	        |   |    SearchExampleServer.java 	- server implementation (using the generated code)
            |   io
            |   	github
            |   		ppissias                        
            |       		xsrpcj
            |           		example
            |               		simple
            |                       	SearchMessages.java -generated by protoc from SearchMessages.proto 	           
	        proto
	                SearchMessages.proto		-our message definition file for the services
	                service-desc.json 			-service description

after invoking the generator as

    java -jar xsrpcj-1.0.0-jar-with-dependencies.jar -server -client -infrastructure src proto/service-desc.json

 we will have

    src
        main
            java
            |   |   SearchExampleClient.java
            |   |   SearchExampleServer.java
            |   |
            |   io
            |   	github
            |   		ppissias                        
            |       		xsrpcj
            |           		example
            |               		simple
            |                   		|   SearchMessages.java
            |                  			|
            |                   	client	-Client generated code 
            |                   		|       PersonsClientService.java		-Client service interface
            |                   		|       PersonsClientServiceImpl.java	-Client service implementation
            |                   		|       PersonsNotifyClientCallback.java	-Client callback interface (to be implemented as a handler) 
            |                   		|
            |                   	comms		-Infrastructure code (low level RPC implementation) 
            |                   		|       ClientReplyHandler.java
            |                   		|       DataHandler.java
            |                   		|       ErrorHandler.java
            |                   		|       RemoteCommunicationsErrorType.java
            |                   		|       RemoteCommunicationsException.java
            |                   		|       ServiceProxy.java
            |                   		|       SocketDataTransceiver.java
            |                   		|       SocketDataTransceiverReaderThread.java
            |                   		|
            |                   	server	-Server Generated code
            |                   		|       PersonsClientHandler.java		-Internal class handling client requests
            |                   		|       PersonsNotifyServerCallback.java	-Server callback interface (implementations of this interface are provided in method calls)
            |                   		|       PersonsServer.java	-The class we use to start the server 
            |                   		|       PersonsServerService.java		-The Server service interface, needs to be implemented in order to define the logic of our services 
            |                   		|
            |                   	types		-Internal data types
            |                           		Persons.java	-Generated Data types from PersonsMessageContainer.proto
            |
            proto
                    PersonsMessageContainer.proto	-Generated (and compiled) internal .proto file. It contains an envelope (packet) that carries the messages from our services
                    SearchMessages.proto
                    service-desc.json


You can navigate the [example source code](https://github.com/ppissias/xsrpcj-examples/tree/master/xsrpcj-simple) to see the content of the files in detail.  
You don't need to know the contents of each file, you are guided in what you need to implement by trying to use the client and server side code. 

### Implementing the client side

when you try to use the generated code in order to invoke services, for example: 

    PersonsClientService serverRef = new PersonsClientServiceImpl( ... ) 
    serverRef.search( ... ) 
you will notice that the constructor of the service implementation (`PersonsClientServiceImpl`):

    PersonsClientServiceImpl(String host, PersonsNotifyClientCallback clientnotifyCallback)
requires a callback handler (`PersonsNotifyClientCallback`) that you need to implement. 
 
### Implementing the server side

when you try to start the server: 

    new PersonsServer( ...).start()

  you will notice that the constructor of the `PersonsServer` :

      public PersonsServer(PersonsServerService serviceHandler)
requires an implementation of the `PersonsServerService` , which defines the service logic. 

In this example, these are the only things you need to do. 

### running invoking the generator programatically
you can invoke the generator by calling the static method

    XsRPCJGenerator.generate
which takes the following arguments:

    public synchronized static void generate(
	    boolean generateClientFiles,  //same as -client : indicating if we will generate the client code
	    boolean generateServerFiles, //same as -server : indicating if we will generate the server code
    	boolean generateInfrastructureFiles, //same as -infrastructure : indicating if we will generate the infrastructure code
    	String jsonFile,  //full or relative path to the service description JSON file
    	String sourcePath, //the source path top level directory, where all source code will be generated, i.e. /path/to/src 
    	String protocPath //the full path to the protoc executable 
    	)

  
## Maven and ant integration 

  
### Ant integration
Below is a simple ant task that calls the generator

	<property name="src.dir" location="src" />
	<property name="proto.src.dir" location="proto" />	

	<!-- define classpath -->
	<path id="compile.rpc.classpath"> 
		<fileset dir="/path/to/xsrpcj"> <!-- xsrpc generator jar with dependencies -->
				<include name="*.jar" />
		</fileset>			
	</path>
	
	<!-- define target-->
	<target name="generateRPC">		
		<java classname="io.github.ppissias.xsrpcj.XsRPCJGenerator" fork="true">
		
		  	<arg value="-server"/>
			<arg value="-client"/>
			<arg value="-infrastructure"/>
			<arg value="${src.dir}"/>
			<arg value="${proto.src.dir}/service-desc.json"/>
			
			<classpath>
				<path refid="compile.rpc.classpath" /> 
			</classpath>
		</java>
	</target>


### Maven integration

You can check the examples (https://github.com/ppissias/xsrpcj-examples) in order to see a pom.xml that uses xsrpcj.

In short:  

	
	<properties>
	    <project.build.sourceProtoDirectory>${project.basedir}/src/main/proto</project.build.sourceProtoDirectory>	
	</properties>

	<dependencies>
	
		<dependency>
			<groupId>io.github.ppissias</groupId>
			<artifactId>xsrpcj</artifactId>
			<version>1.0.0</version>
		</dependency>

		...
		
	</dependencies>
	
	<build>
		<sourceDirectory>src/main/java</sourceDirectory>
		.... 

				<plugin>
					<groupId>org.codehaus.mojo</groupId>
					<artifactId>exec-maven-plugin</artifactId>
					<version>1.6.0</version>
					<executions>

						<!--  generate RPC java code and compile generated .proto file with the xsrpcj generator-->
						<execution>
							<id>generate RPC stubs</id>
							<phase>generate-sources</phase>
							<configuration>
								<mainClass>io.github.ppissias.xsrpcj.XsRPCJGenerator</mainClass>
								<arguments>
									<argument>-server</argument>
									<argument>-client</argument>
									<argument>-infrastructure</argument>
									<argument>${project.build.sourceDirectory}</argument>
									<argument>${project.build.sourceProtoDirectory}/service-desc.json</argument>
								</arguments>
							</configuration>
							<goals>
								<goal>java</goal>
							</goals>
						</execution>

					</executions>
				</plugin>
  


## Contributing

  

Please feel free to extend the project!

  

## License

  

This project is licensed under the MIT License (https://opensource.org/licenses/MIT)
- see the [LICENSE](LICENSE) file for details


  

## Acknowledgments

  

* Thanks to the protocol buffers developers, the velocity engine developers and the gson developers ! 

