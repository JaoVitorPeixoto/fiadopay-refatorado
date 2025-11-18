package edu.ucsal.fiadopay.mapper;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getStatus().name(),
                p.getMethod(),
                p.getAmount(),
                p.getInstallments(),
                p.getMonthlyInterest(),
                p.getTotalWithInterest()
        );
    }
}
