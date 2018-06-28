package ${server.javaPackage}.$serverSubPackage;

#foreach( $importClass in $classImports )
import ${importClass};
#end


/**
 * Service interface on the server-side.
 * 
 * @author Petros Pissias
 *
 */
public interface $className {

	#foreach( $service in $server.services )
		#if ($service.hasResponse() && $service.hasCallback()) 
	public ${service.responseClassName} ${service.serviceName}(${service.requestClassName} request, ${server.name}${service.serviceNameUpper}ServerCallback callback);	
		
		#elseif ($service.hasResponse() && !$service.hasCallback())
	public ${service.responseClassName} ${service.serviceName}(${service.requestClassName} request);		
		
		#elseif (!$service.hasResponse() && !$service.hasCallback())
	public void ${service.serviceName}(${service.requestClassName} request);		
	
	
		#end
	#end
}
