package edu.ucsal.fiadopay.service.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import edu.ucsal.fiadopay.dto.request.PaymentRequest;

@Service
public class NoInterestCalculator implements InterestCalculator {
   
	@Override
    public boolean supports(PaymentRequest req) {
        return true;
    }

    @Override
    public BigDecimal calculateTotal(PaymentRequest req) {
        return req.amount();
    }

    @Override
    public Double getMonthlyInterest(PaymentRequest req) {
        return null;
    }
}	
