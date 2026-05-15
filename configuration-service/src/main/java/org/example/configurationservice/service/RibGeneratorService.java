package org.example.configurationservice.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import org.example.configurationservice.model.MicrofinanceConfiguration;

import java.math.BigInteger;

@Slf4j
@Service
public class RibGeneratorService {

    private final MicrofinanceConfigurationService configService;
    
    
    public RibGeneratorService(@Lazy MicrofinanceConfigurationService configService) {
        this.configService = configService;
    }
    
    public String generateRib(String accountNumber, String clientId) {
        MicrofinanceConfiguration config = configService.getActiveConfiguration();
        
        String finalBankCode = config.isAffiliatedToBank() ? config.getAffiliatedBankCode() : config.getMicrofinanceCode();
        String finalAgencyCode = config.getAgencyCode();
        
        String ribKey = finalBankCode + finalAgencyCode + accountNumber + clientId;
        while (ribKey.length() < 21) ribKey += "0";
        if (ribKey.length() > 21) ribKey = ribKey.substring(0, 21);
        
        String controlKey = calculateControlKey(ribKey);
        return finalBankCode + finalAgencyCode + accountNumber + clientId + controlKey;
    }
    
    private String calculateControlKey(String ribKey) {
        try {
            BigInteger number = new BigInteger(ribKey);
            int controlKey = 97 - number.mod(BigInteger.valueOf(97)).intValue();
            return String.format("%02d", controlKey);
        } catch (Exception e) {
            log.error("calcul clé RIB: {}", e.getMessage());
            return "00";
        }
    }
    
    public String formatRib(String rib) {
        if (rib == null || rib.length() != 23) return rib;
        return rib.substring(0, 5) + " " + rib.substring(5, 10) + " " + 
               rib.substring(10, 16) + " " + rib.substring(16, 21) + " " + rib.substring(21);
    }
    
    public boolean validateRib(String rib) {
        String cleanRib = rib.replaceAll("\\s", "");
        if (cleanRib.length() != 23) return false;
        return calculateControlKey(cleanRib.substring(0, 21)).equals(cleanRib.substring(21));
    }
}