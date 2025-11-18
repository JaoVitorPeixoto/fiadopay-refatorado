package edu.ucsal.fiadopay.service.signature;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Qualifier("hmac")
public class HmacSignatureService implements SignatureService {

    @Override
    public String sign(String payload, String secret){
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
        } catch (Exception e){ return ""; }
    }
}
