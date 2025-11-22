package edu.ucsal.fiadopay.aspect;

import edu.ucsal.fiadopay.annotation.AntiFraud;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@Aspect
@Component
public class AntiFraudAspect {

    @Before("@annotation(antiFraud)")
    public void checkFraud(JoinPoint joinPoint, AntiFraud antiFraud) {
        Object[] args = joinPoint.getArgs();
        BigDecimal amount = findAmountInArgs(args);

        if (amount != null) {
            BigDecimal limit = BigDecimal.valueOf(antiFraud.threshold());
            
            if (amount.compareTo(limit) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Block Antifraud: " + antiFraud.name());
            }
        }
    }

    private BigDecimal findAmountInArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof PaymentRequest) {
                return ((PaymentRequest) arg).amount();
            }
            if (arg instanceof BigDecimal) {
                return (BigDecimal) arg;
            }
        }
        return null;
    }
}