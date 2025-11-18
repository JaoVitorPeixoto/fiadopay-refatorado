package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.dto.request.MerchantCreateRequest;
import edu.ucsal.fiadopay.dto.response.MerchantCreateResponse;
import edu.ucsal.fiadopay.mapper.MerchantMapper;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantMapper merchantMapper;

    public MerchantService(MerchantRepository merchantRepository, MerchantMapper merchantMapper) {
        this.merchantRepository = merchantRepository;
        this.merchantMapper = merchantMapper;
    }

    public MerchantCreateResponse create(MerchantCreateRequest dto) {
        if (existsByName(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Merchant name already exists");
        }
        var m = Merchant.builder()
                .name(dto.name())
                .webhookUrl(dto.webhookUrl())
                .clientId(UUID.randomUUID().toString())
                .clientSecret(UUID.randomUUID().toString().replace("-", ""))
                .status(Merchant.Status.ACTIVE)
                .build();

        return merchantMapper.toCreatorResponse(merchantRepository.save(m));
    }

    public Optional<Merchant> findByClientId(String clientId) {
        return merchantRepository.findByClientId(clientId);
    }

    public boolean existsByName(String name){
        return merchantRepository.existsByName(name);
    }


}
