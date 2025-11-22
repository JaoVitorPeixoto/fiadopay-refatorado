package edu.ucsal.fiadopay.annotation.logador;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLogAspect extends TemplateLog {

	@Before("within(@org.springframework.stereotype.Service *)")
	public void logService(JoinPoint joinPoint) {
		log(joinPoint, "Service");
	}
}

//
//        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
//
//        Class<?> serviceClass = sig.getDeclaringType();			//Pega a Classe
//        String className = serviceClass.getSimpleName();		//Pega o nome da classe
//        String methodName = sig.getMethod().getName();			//Pega o metodo invocado
//
//        Logger logger = LoggerFactory.getLogger(serviceClass);	//Define o log para a classe utilizada.
//
//        logger.info("----- Service / {}.{}() -----", className, methodName);	//Logga a classe e o metodo utilizado