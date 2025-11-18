package edu.ucsal.fiadopay.controller;

import edu.ucsal.fiadopay.dto.request.MerchantCreateRequest;
import edu.ucsal.fiadopay.dto.response.MerchantCreateResponse;
import edu.ucsal.fiadopay.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fiadopay/admin/merchants")
@RequiredArgsConstructor
public class MerchantAdminController {
  private final MerchantService merchantService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MerchantCreateResponse create(@Valid @RequestBody MerchantCreateRequest dto) {
    return merchantService.create(dto);
  }
}
