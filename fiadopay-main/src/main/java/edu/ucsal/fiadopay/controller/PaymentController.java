package edu.ucsal.fiadopay.controller;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.dto.request.PaymentRequest;
import edu.ucsal.fiadopay.dto.request.RefundRequest;
import edu.ucsal.fiadopay.dto.response.PaymentResponse;
import edu.ucsal.fiadopay.service.payment.PaymentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/fiadopay/gateway")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentService service;

  @PostMapping("/payments")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PaymentResponse> create(
    @RequestAttribute("merchant") Merchant merchant,
    @RequestHeader(value="Idempotency-Key", required=false) String idemKey,
    @RequestBody @Valid PaymentRequest req
  ) {
    var resp = service.createPayment(merchant, idemKey, req);
    return ResponseEntity.status(HttpStatus.CREATED).body(resp);
  }

  @GetMapping("/payments/{id}")
  public PaymentResponse get(@PathVariable String id) {
    return service.getPayment(id);
  }

  @PostMapping("/refunds")
  @SecurityRequirement(name = "bearerAuth")
  public java.util.Map<String,Object> refund(
    @Parameter(hidden = true) 
    @RequestAttribute("merchant") Merchant merchant,
    @RequestBody @Valid RefundRequest body) {
    return service.refund(merchant, body.paymentId());
  }
}
