package edu.ucsal.fiadopay.service.payment;

import edu.ucsal.fiadopay.annotation.PaymentMethod;
import edu.ucsal.fiadopay.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentType, InterestCalculator> strategies = new EnumMap<>(PaymentType.class);

    public PaymentStrategyFactory(List<InterestCalculator> calculators) {
        for (InterestCalculator calc : calculators) {
            if (calc.getClass().isAnnotationPresent(PaymentMethod.class)) {
                PaymentMethod annotation = calc.getClass().getAnnotation(PaymentMethod.class);

                for (PaymentType type : annotation.types()) {
                    strategies.put(type, calc);
                }
            }
        }
    }

    public InterestCalculator getCalculator(PaymentType type) {
        return strategies.get(type);
    }
}