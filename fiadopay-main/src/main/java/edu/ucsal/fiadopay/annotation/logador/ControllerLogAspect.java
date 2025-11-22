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
