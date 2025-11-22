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
