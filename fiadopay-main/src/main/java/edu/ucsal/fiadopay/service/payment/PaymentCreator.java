package edu.ucsal.fiadopay.service.payment;

import java.time.Instant;
import java.util.UUID;

import edu.ucsal.fiadopay.annotation.AntiFraud;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.PaymentType;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCreator {

    private final PaymentRepository payments;
    private final PaymentStrategyFactory strategyFactory; // Injeção da Factory

    public PaymentRepository getPayments() {
        return payments;
    }

    // Validação automática via Aspecto
    @AntiFraud(name = "HighValueCheck", threshold = 10000.0)
    public Payment create(String idemKey, Long merchantId, PaymentRequest req) {

        if (idemKey != null) {
            var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, merchantId);
            if (existing.isPresent()) return existing.get();
        }

        // Converte String para Enum com segurança
        PaymentType type;
        try {
            type = PaymentType.valueOf(req.method().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            type = PaymentType.UNKNOWN; 
        }

        // Busca a calculadora certa no mapa (O(1))
        InterestCalculator calc = strategyFactory.getCalculator(type);
        
        if (calc == null) {
             throw new IllegalStateException("Método de pagamento não suportado: " + type);
        }

        var payment = Payment.builder()
            .id("pay_" + UUID.randomUUID().toString().substring(0, 8))
            .merchantId(merchantId)
            .method(type.name())
            .amount(req.amount())
            .currency(req.currency())
            .installments(req.installments() == null ? 1 : req.installments())
            .monthlyInterest(calc.getMonthlyInterest(req))
            .totalWithInterest(calc.calculateTotal(req))
            .status(Payment.Status.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .idempotencyKey(idemKey)
            .metadataOrderId(req.metadataOrderId())
            .build();

        payments.save(payment);
        return payment;
    }
}