package edu.ucsal.fiadopay.annotation.payment;

import java.math.BigDecimal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PaymentAspect {

    @Around("@annotation(payment)")
    public Object processPayment(ProceedingJoinPoint pjp, PaymentAnnotation payment) throws Throwable {

        // executa o método original
        Object result = pjp.proceed();

        if (result == null || !(result instanceof BigDecimal)) {    //Valida se existe e se é dinheiro
            return result;
        }

        BigDecimal ajustado = applyRules((BigDecimal)result, payment.type());   //Aplica a regra de negocio
        return ajustado;
    
    }

    private BigDecimal applyRules(BigDecimal value, PaymentType type) {
        return switch (type) {
            case PIX -> value.multiply(BigDecimal.valueOf(0.9));         // 10% de desconto
            case CARD -> value.add(BigDecimal.valueOf(5.0));            // taxa fixa de 5 reais
        };
    }
}
