package edu.ucsal.fiadopay.annotation.logador;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerLogAspect extends TemplateLog {

	@Before("within(@org.springframework.stereotype.Controller *) || "
			+ "within(@org.springframework.web.bind.annotation.RestController *)")
	public void logController(JoinPoint joinPoint) {
		log(joinPoint, "Controller");

	}
}

// Qualquer método dentro de classe anotada com @Controller OU @RestController
//        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
//
//        Class<?> controllerClass = sig.getDeclaringType();
//        String className = controllerClass.getSimpleName();
//        String methodName = sig.getMethod().getName();
//
//        Logger logger = LoggerFactory.getLogger(controllerClass);
//
//        logger.info("----- Controller / {}.{}() -----", className, methodName);