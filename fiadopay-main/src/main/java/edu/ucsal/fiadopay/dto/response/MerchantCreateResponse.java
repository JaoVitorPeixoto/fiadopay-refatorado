package edu.ucsal.fiadopay.dto.response;

import edu.ucsal.fiadopay.domain.Merchant;

public record MerchantCreateResponse(
        String name,
        String webhookUrl,
        String clientId,
        String clientSecret,
        Merchant.Status status
){}
