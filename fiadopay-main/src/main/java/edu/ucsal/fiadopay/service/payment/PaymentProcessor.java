package edu.ucsal.fiadopay.service.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.PaymentRepository;

@Service
public class PaymentProcessor {

    @Value("${fiadopay.processing-delay-ms}")
    private long delay;

    @Value("${fiadopay.failure-rate}")
    private double failRate;

    private final PaymentRepository payments;

    public PaymentProcessor(PaymentRepository payments) {
        this.payments = payments;
    }

    public Payment process(String paymentId) {

        try { Thread.sleep(delay); } catch (InterruptedException ignored) {}

        var p = payments.findById(paymentId).orElse(null);
        if (p == null) return null;

        var approved = Math.random() > failRate;
        p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        return p;
    }
}

