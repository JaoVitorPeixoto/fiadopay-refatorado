package edu.ucsal.fiadopay.mapper;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.dto.response.MerchantCreateResponse;
import org.springframework.stereotype.Component;

@Component
public class MerchantMapper {

    public MerchantCreateResponse toCreatorResponse(Merchant m){
        return new MerchantCreateResponse(
            m.getName(),
            m.getWebhookUrl(),
            m.getClientId(),
            m.getClientSecret(),
            m.getStatus()
        );
    }
}
