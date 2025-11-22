package edu.ucsal.fiadopay.annotation;

import edu.ucsal.fiadopay.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface PaymentMethod {
    PaymentType[] types();
}