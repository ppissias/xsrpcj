

# xsrpcj

  

**xsrpcj** is an extra small (xs) and ultra fast remote procedure call (rpc) implementation for java on top of TCP Sockets, It is designed to be used with **protocol buffers** generated data types but it can also use custom data types. It supports 3 interaction patterns:

  

- **Oneway**: similar to a void function. We just send some data.

- **Request/Reply**: we send some data and get some data back as a reply

- **Request/Reply/Callback**: in addition to the request/reply interaction pattern, we also get asynchronously a number of callback messages.

  

A typical example of the request/reply/callback interaction pattern is that we make an interaction (for example a registration) and then as a result of this interaction we get back asynchronously data from time to time.

  

xsrpcj focuses on a **simple programming model** and **minimal dependencies**. In fact, you don't need anything else except protocol buffers, not even xsrpcj itself, since it generates all the code you need.

  

## Getting Started

  

OK, so you have in mind some **services** you want to **implement**. You need to **express** these services in terms of **interaction patterns** and create the **data types** for the various requests replies and possible callbacks. A **typical workflow** using xsrpcj is:

  

- You **describe your services** (this is done in a **JSON** file)

- You **create** your **data types** (either using **protocol buffers**, or using your own custom data types - as long as they obey some simple rules, explained later)

- You invoke the **xsrpcj code generator**

**That's it.** xsrpcj will generate a client API and and all the necessary server-side code.

  

Lets take it **step by step**.



  

### Service Description

Services are described in a JSON file. A simple example is provided below:

    {
	"servers":[
	 	{
			"name": "Persons",
			"port": "22100",
			"javaPackage":"org.xsrpcj.example.simple",
			"services":[
				{"serviceName":"search", "requestType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonRequest", "responseType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" },
				{"serviceName":"notify", "requestType":"org.xsrpcj.example.simple.SearchMessages.PersonNotificationRequest", "responseType":"org.xsrpcj.example.simple.SearchMessages.PersonNotificationResponse" , "callbackType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" }
			]
		}		
	]
		}		
	]
	} 




Services are grouped into Servers. Each server implements a set of services and listens on a specific TCP port.
In the example above, we define 2 services realized by a server named "Persons". 

Lets look closer at the service definition, each service has : 

- a **name** : to identify the server (this will be used for the generation of the name of the method in both the client and server APIs, in combination with the Server name)

- a **request type**: This is the data type that is sent to the server when invoking the service (the request data)

- an **optional response type**: This is the data type we get back as a reply when invoking the service

- an **optional callback type**: This is the data type we get back asynchronously from time to time as a result of invoking the service

  

after invoking the xsrpcj code generator, we will get a server-side API and a client-side API. For the client-side we will get an interface & its implementation for each Server, which is all we need to start invoking services.

**Client Side**

For the example above we will get  a client side interface:

    public interface PersonsClientService {

	public SearchPersonResponse search(SearchPersonRequest request) throws RemoteCommunicationsException;		
		
	public PersonNotificationResponse notify(PersonNotificationRequest request) throws RemoteCommunicationsException;
	}		
		

		


and its implementation (which implements the low level RPC and encoding stuff).  


From the client side, the way to start invoking services is :

 
		//get a service reference
		PersonsClientService serverRef = new PersonsClientServiceImpl(serverHost, cbHandler);			

		
		//now we can use it to make calls on the server
		
		//request / response call
		SearchPersonResponse resp = serverRef.search(....);
		
		//request / response / callback call
		PersonNotificationResponse notResp = serverRef.notify(....);

					
Notice that we need to pass a "callback handler" (`cbHandler`) when we instantiate `ExampleClientServiceImpl` ? We need to do this for each service that implements a callback. This is needed because we need to provide a handler for the asynchronous callback messages. 

So if we used no services with callbacks, the constructor would not need a callback handler and if we used 10 services with callbacks, the constructor would need 10 callback handlers, one for each service. 

In our particular case, we only have 1 service with a callback, so we need to provide just 1 callback handler. 
The callback handler needs to implement an interface which defines a method able to process the callback  messages. In our particular example it looks like:

    public interface PersonsNotifyClientCallback {
		public void notifyCallback(SearchPersonResponse cb);
	}


The naming convention comes from the Server and Service name descriptions, which can be as small and simple as you like, or long if you like detailed names.     

Besides the callback, we pass an additional argument, `serverHost`which is the host where the service is implemented (we can also pass a port argument, if the server listens on a different port than the default)

    ExampleClientService serverRefDefaultPort = new ExampleClientServiceImpl("localhost", cbHandler);
    	
Even though the service description already has a predefined port, on the client side we have the flexibility to override this setting.:

    ExampleClientService serverRef = new ExampleClientServiceImpl("localhost", 22100, cbHandler);
    

**Server side**
  
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
		
}
		
It is the kind of interface that you would expect, according to the service description. Notice that you receive a callback object on method `notify` that you can use in order to send asynchronously responses to the caller. The underlying implementation is automatically generated and will route back the reply to the client callback handler on the client side. 

That's it ! You can see the full example source code here for the 

 - Proto message definition file :
 - Client Implementation : 
 - Server Implementation :

  
## Examples

Download the xsrpcj examples project which contains several examples on using xsrpcj and also examples that can be used to compare the performance of xsrpcj and grpc for the same services.


## Compiling

    mvn clean compile assembly:single
will compile everything and assemble it as a single executable jar. You can then use the produced .jar file (see section below) in order to generate your RPC code.  You can also use it through ant and maven (see sections below)

## Using the generator 
The generator can be used in 2 ways: 

 - by running the produced .jar and providing the required arguments
	 - maven, ant & gradle integration is easy (see next chapters)
 - programatically by invoking a static method
 

Lets see both of them in detail: 

**running the produced .jar (standalone or via ant / maven / ... ) ** : 

    java -jar xsrpcgen-1.0-SNAPSHOT-jar-with-dependencies.jar
    
    expected arguments: [-client] [-server] [-infrastructure] <source generation path> <service-description-file>
    
for example assuming that you have the following service description file in `proto/service-description.json`

    {
	"servers":[
	 	{
			"name": "Persons",
			"port": "22100",
			"javaPackage":"org.xsrpcj.example.simple",
			"services":[
				{"serviceName":"search", "requestType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonRequest", "responseType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" },
				{"serviceName":"notify", "requestType":"org.xsrpcj.example.simple.SearchMessages.PersonNotificationRequest", "responseType":"org.xsrpcj.example.simple.SearchMessages.PersonNotificationResponse" , "callbackType":"org.xsrpcj.example.simple.SearchMessages.SearchPersonResponse" }
			]
		}		
	], 
	"infrastructure" : {
		"javaPackage":"org.xsrpcj.example.simple.comms",
		"logging":"System"
	}
	}	 



when you invoke the code generator with the following arguments: 

        java -jar xsrpcgen-1.0-SNAPSHOT.jar -server -client -infrastructure src proto/service-desc.json 


It will generate the required code for the server, client and the common infrastructure files (used for the low level communication). The infrastructure files are needed only once, so if you generate multiple services on the same application you do not need to generate them again and again. 

In detail it will generate: 

 - org.xsrpcj.example.simple.comms package with all required classes (which are the common infrastructure for the client, server and the communication between them) 
 - org.xsrpcj.example.simple.client package with all the client generated code and interfaces
 - org.xsrpcj.example.simple.server with all the server generated code and interfaces 
 - org.xsrpcj.example.simple.types which contains some internal automatically generated data types. The generator generates and compiles its own little .proto file and the resulting types are generated in this package.

  

  
## Maven and ant integration 

  
### Ant integration
Below is a simple ant task that calls the generator

	<target name="generateRPC" depends="cleanRPC, generateProtobuf">		
		<java classname="org.xsrpc.gen.XsRPCJGenerator" fork="true">
		
		  	<arg value="-server"/>
			<arg value="-client"/>
			<arg value="-infrastructure"/>
			<arg value="${src.dir}"/>
			<arg value="${proto.src.dir}/service-desc.json"/>
			
			<classpath>
				<path refid="compile.rpc.classpath" /> <!-- here you need to have the generator jar in your classpath -->
			</classpath>
		</java>
	</target>

### Maven integration 
 ... comming soon ...

  


## Contributing

  

Please read feel free to extend the project!

  


  

## License

  

This project is licensed under the GNU LESSER GENERAL PUBLIC LICENSE
                       Version 3, - see the [LICENSE](LICENSE) file for details

  

## Acknowledgments

  

* Thanks of course to the protocol buffers developers, the velocity engine developers and the gson developers ! 


