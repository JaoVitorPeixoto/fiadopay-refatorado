package edu.ucsal.fiadopay.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import edu.ucsal.fiadopay.annotation.PaymentMethod;
import edu.ucsal.fiadopay.domain.PaymentType;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;

@Service
@PaymentMethod(types = { PaymentType.CARD })
public class CardInterestCalculator implements InterestCalculator {

    @Override
    public BigDecimal calculateTotal(PaymentRequest req) {
        var base = new BigDecimal("1.01");
        var factor = base.pow(req.installments());
        return req.amount()
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Double getMonthlyInterest(PaymentRequest req) {
        return 1.0;
    }
}