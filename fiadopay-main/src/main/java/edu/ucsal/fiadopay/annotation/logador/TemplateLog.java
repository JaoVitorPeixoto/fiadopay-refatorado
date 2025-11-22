package edu.ucsal.fiadopay.annotation.logador;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TemplateLog {

	protected void log(JoinPoint joinPoint, String layerName) { // Template Method
		MethodSignature sig = (MethodSignature) joinPoint.getSignature(); // Pega a assinatura (id/key) do metodo

		Class<?> targetClass = sig.getDeclaringType(); // Pega a declaração da Classe
		String className = targetClass.getSimpleName(); // Pega o nome da Classe Declarada pelo Metodo
		String methodName = sig.getMethod().getName(); // Pega o nome do metodo

		Logger logger = LoggerFactory.getLogger(targetClass); // Define para fazer um logo com a referencia da classe
																// Logada

		logger.info("----- {} / {}.{}() -----", layerName, className, methodName); // Faz o log
	}
}
