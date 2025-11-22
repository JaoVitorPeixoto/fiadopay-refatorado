package edu.ucsal.fiadopay.service.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import edu.ucsal.fiadopay.annotation.PaymentMethod;
import edu.ucsal.fiadopay.domain.PaymentType;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;

@Service
@PaymentMethod(types = { PaymentType.DEBIT, PaymentType.PIX, PaymentType.BOLETO })
public class NoInterestCalculator implements InterestCalculator {
   
    @Override
    public BigDecimal calculateTotal(PaymentRequest req) {
        return req.amount();
    }

    @Override
    public Double getMonthlyInterest(PaymentRequest req) {
        return 0.0;
    }
}