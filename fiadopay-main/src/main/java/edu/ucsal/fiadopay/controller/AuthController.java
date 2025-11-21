package edu.ucsal.fiadopay.controller;

import edu.ucsal.fiadopay.annotation.Public;
import edu.ucsal.fiadopay.dto.request.TokenRequest;
import edu.ucsal.fiadopay.dto.response.TokenResponse;
import edu.ucsal.fiadopay.service.auth.AuthService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fiadopay/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @Public
  @PostMapping("/token")
  public TokenResponse token(@RequestBody @Valid TokenRequest req) {
    return authService.generateToken(req);
  }
}
