package edu.ucsal.fiadopay.service.auth;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.dto.request.TokenRequest;
import edu.ucsal.fiadopay.dto.response.TokenResponse;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantRepository merchantRepository;

    public Merchant merchantFromAuth(String auth){
        if (auth == null || !auth.startsWith("Bearer FAKE-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var raw = auth.substring("Bearer FAKE-".length());
        long id;
        try {
            id = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var merchant = merchantRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (merchant.getStatus() != Merchant.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return merchant;
    }

    public TokenResponse generateToken(TokenRequest req) {
        var merchant = merchantRepository.findByClientId(req.client_id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!merchant.getClientSecret().equals(req.client_secret()) || merchant.getStatus()!= Merchant.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return new TokenResponse("FAKE-"+merchant.getId(), "Bearer", 3600);
    }
}
