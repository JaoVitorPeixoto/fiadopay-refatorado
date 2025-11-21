package edu.ucsal.fiadopay.controller;

import edu.ucsal.fiadopay.annotation.Public;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {
  @Public
  @GetMapping("/fiadopay/health")
  public Map<String,String> health() {
    return Map.of("status","UP");
  }
}
