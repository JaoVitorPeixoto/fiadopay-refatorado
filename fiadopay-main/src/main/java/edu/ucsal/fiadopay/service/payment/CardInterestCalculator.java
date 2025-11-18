package edu.ucsal.fiadopay.service.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import edu.ucsal.fiadopay.dto.request.PaymentRequest;

@Service
public class CardInterestCalculator implements InterestCalculator {
	
	@Override
    public boolean supports(PaymentRequest req) {
        return "CARD".equalsIgnoreCase(req.method()) &&
               req.installments() != null &&
               req.installments() > 1;
    }

    @Override
    public BigDecimal calculateTotal(PaymentRequest req) {
        var base = new BigDecimal("1.01");
        var factor = base.pow(req.installments());
        return req.amount().multiply(factor).setScale(2);
    }

    @Override
    public Double getMonthlyInterest(PaymentRequest req) {
        return 1.0;
    }
}
