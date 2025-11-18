package edu.ucsal.fiadopay.service.payment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;
import edu.ucsal.fiadopay.mapper.PaymentMapper;
import edu.ucsal.fiadopay.repo.PaymentRepository;

public class PaymentCreator {
	
	private final PaymentRepository payments;
    private final PaymentMapper mapper;
    private final List<InterestCalculator> calculators;

    public PaymentCreator(PaymentRepository payments, PaymentMapper mapper, List<InterestCalculator> calculators) {
        this.payments = payments;
        this.mapper = mapper;
        this.calculators = calculators;
    }

    public Payment create(Long idemKey, String merchantId, PaymentRequest req) {

        if (idemKey != null) {
            var existing = payments.findByIdempotencyKeyAndMerchantId(merchantId, idemKey);
            if (existing.isPresent()) return existing.get();
        }

        var calc = calculators.stream()
                .filter(c -> c.supports(req))
                .findFirst()
                .orElseThrow();

        var payment = Payment.builder()
            .id("pay_" + UUID.randomUUID().toString().substring(0, 8))
            .merchantId(merchantId)
            .method(req.method().toUpperCase())
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
