package edu.ucsal.fiadopay.service.signature;


public interface SignatureService {
    String sign(String payload, String secret);
}
