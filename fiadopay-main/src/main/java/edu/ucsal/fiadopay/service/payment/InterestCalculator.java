package edu.ucsal.fiadopay.service.payment;

import java.math.BigDecimal;

import edu.ucsal.fiadopay.dto.request.PaymentRequest;

public interface InterestCalculator {
    BigDecimal calculateTotal(PaymentRequest req);
    Double getMonthlyInterest(PaymentRequest req);
}
