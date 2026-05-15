package org.example.configurationservice.service;

import org.example.configurationservice.dto.reponse.MicrofinanceConfigurationResponse;
import org.example.configurationservice.dto.request.AccountNumberFormatRequest;
import org.example.configurationservice.dto.request.MicrofinanceConfigurationRequest;
import org.example.configurationservice.exceptions.ConfigurationNotFoundException;
import org.example.configurationservice.model.MicrofinanceConfiguration;
import org.example.configurationservice.model.enums.ClientIdGenerationStrategy;
import org.example.configurationservice.repository.MicrofinanceConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicrofinanceConfigurationService {
    
    private final MicrofinanceConfigurationRepository configRepository;
    private final RibGeneratorService ribGeneratorService;
    private final AtomicLong sequentialCounter = new AtomicLong(1);
    
    @Transactional
    public MicrofinanceConfigurationResponse saveConfiguration(MicrofinanceConfigurationRequest request, String createdBy) {
        log.info("de la configuration microfinance");
        
        //  l'ancienne configuration active
        configRepository.findByActiveTrue().ifPresent(oldConfig -> {
            oldConfig.setActive(false);
            configRepository.save(oldConfig);
        });
        
        String finalBankCode = request.isAffiliatedToBank() ? request.getAffiliatedBankCode() : request.getMicrofinanceCode();
        
        ClientIdGenerationStrategy strategy = request.getClientIdStrategy();
        if (strategy == null) {
            strategy = ClientIdGenerationStrategy.SEQUENTIAL;
        }
        
        MicrofinanceConfiguration config = MicrofinanceConfiguration.builder()
                .microfinanceCode(request.getMicrofinanceCode())
                .affiliatedBankCode(request.getAffiliatedBankCode())
                .affiliatedToBank(request.isAffiliatedToBank())
                .clientIdStrategy(strategy)
                .customClientIdPattern(request.getCustomClientIdPattern())
                .enableRibGeneration(request.isEnableRibGeneration())
                .bankCode(finalBankCode)
                .countryCode(request.getCountryCode())
                .controlKeyAlgorithm(request.getControlKeyAlgorithm())
                .active(true)
                .createdBy(createdBy)
                .build();
        
        config = configRepository.save(config);
        log.info("microfinance enregistrée: {}", config.getMicrofinanceCode());
        
        return mapToResponse(config);
    }
    
    @Transactional(readOnly = true)
    public MicrofinanceConfiguration getActiveConfiguration() {
        return configRepository.findByActiveTrue()
                .orElseThrow(() -> new ConfigurationNotFoundException("Aucune configuration active trouvée"));
    }
    
    @Transactional(readOnly = true)
    public MicrofinanceConfigurationResponse getActiveConfigurationResponse() {
        return mapToResponse(getActiveConfiguration());
    }
    
    // 
    //  POUR LE FORMAT DE NUMÉRO DE COMPTE
    // 
    
    @Transactional
    public MicrofinanceConfigurationResponse configureAccountNumberFormat(AccountNumberFormatRequest request) {
        log.info("du format de numéro de compte");
        
        MicrofinanceConfiguration config = getActiveConfiguration();
        
        config.setUseCustomFormat(request.isUseCustomFormat());
        config.setAccountNumberFormat(request.getAccountNumberFormat());
        config.setBankCodeLength(request.getBankCodeLength());
        config.setAccountNumberLength(request.getAccountNumberLength());
        config.setControlKeyLength(request.getControlKeyLength());
        config.setSeparator(request.getSeparator());
        config.setFixedPrefix(request.getFixedPrefix());
        config.setFixedSuffix(request.getFixedSuffix());
        config.setGenerationStrategy(request.getGenerationStrategy());
        config.setIncludeCheckDigit(request.isIncludeCheckDigit());
        
        config = configRepository.save(config);
        
        return mapToResponse(config);
    }
    
    @Transactional(readOnly = true)
    public AccountNumberFormatRequest getAccountNumberFormat() {
        MicrofinanceConfiguration config = getActiveConfiguration();
        
        AccountNumberFormatRequest request = new AccountNumberFormatRequest();
        request.setUseCustomFormat(config.isUseCustomFormat());
        request.setAccountNumberFormat(config.getAccountNumberFormat());
        request.setBankCodeLength(config.getBankCodeLength());
        request.setAccountNumberLength(config.getAccountNumberLength());
        request.setControlKeyLength(config.getControlKeyLength());
        request.setSeparator(config.getSeparator());
        request.setFixedPrefix(config.getFixedPrefix());
        request.setFixedSuffix(config.getFixedSuffix());
        request.setGenerationStrategy(config.getGenerationStrategy());
        request.setIncludeCheckDigit(config.isIncludeCheckDigit());
        
        return request;
    }
    
    @Transactional(readOnly = true)
    public String testAccountNumberGeneration(String clientId) {
        log.info("de génération de numéro de compte pour client: {}", clientId);
        return generateAccountNumber(clientId);
    }
    
    @Transactional(readOnly = true)
    public String formatAccountNumber(String accountNumber, String agencyCode) {
        MicrofinanceConfiguration config = getActiveConfiguration();
        return formatAccountNumber(accountNumber, config, agencyCode);
    }

    
    @Transactional(readOnly = true)
    public String formatAccountNumber(String accountNumber) {
       MicrofinanceConfiguration config = getActiveConfiguration();
       return formatAccountNumber(accountNumber, config, null);
    }
    
    // 
    //  DE GÉNÉRATION D'ID CLIENT
    // 
    
    @Transactional
    public String generateClientId(String clientEmail) {
        MicrofinanceConfiguration config = getActiveConfiguration();
        ClientIdGenerationStrategy strategy = config.getClientIdStrategy();
        
        if (strategy == null) {
            strategy = ClientIdGenerationStrategy.SEQUENTIAL;
        }
        
        switch (strategy) {
            case RANDOM:
                return generateRandomClientId();
            case SEQUENTIAL:
                return generateSequentialClientId();
            case CUSTOM:
                return generateCustomClientId(clientEmail, config);
            case MIXED:
                return generateMixedClientId(config);
            default:
                return generateRandomClientId();
        }
    }
    
    @Transactional
    public boolean validateCustomClientId(String customId) {
        return customId != null && customId.length() == 11 && customId.matches("\\d{11}");
    }
    
    // 
    //  PRIVÉES DE GÉNÉRATION DE NUMÉRO DE COMPTE
    // 
    
    /**
     * Génère un numéro de compte (sans code agence, car géré par Account Service)
     */
    private String generateAccountNumber(String clientId) {
        MicrofinanceConfiguration config = getActiveConfiguration();
        
        if (config.isUseCustomFormat() && config.getAccountNumberFormat() != null) {
            return generateFromCustomFormat(config, clientId);
        } else {
            return generateStandardFormat(config, clientId);
        }
    }
    
    private String generateFromCustomFormat(MicrofinanceConfiguration config, String clientId) {
        String format = config.getAccountNumberFormat();
        
        String result = format
                .replace("{BANK}", generateBankCode(config))
                .replace("{AGENCY}", "{AGENCY}")  //  pour Account Service
                .replace("{ACCOUNT}", generateAccountNumberPart(config))
                .replace("{CLIENT}", getShortClientId(clientId, config.getAccountNumberLength()))
                .replace("{KEY}", generateControlKey(config))
                .replace("{PREFIX}", config.getFixedPrefix() != null ? config.getFixedPrefix() : "")
                .replace("{SUFFIX}", config.getFixedSuffix() != null ? config.getFixedSuffix() : "")
                .replace("{DATE}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .replace("{YEAR}", String.valueOf(LocalDateTime.now().getYear()))
                .replace("{MONTH}", String.format("%02d", LocalDateTime.now().getMonthValue()))
                .replace("{DAY}", String.format("%02d", LocalDateTime.now().getDayOfMonth()))
                .replace("{TIMESTAMP}", String.valueOf(System.currentTimeMillis()).substring(5));
        
        String separator = config.getSeparator();
        if (separator == null || separator.isEmpty()) {
            result = result.replace("-", "").replace("_", "");
        }
        
        return result;
    }
    
    private String generateStandardFormat(MicrofinanceConfiguration config, String clientId) {
        String bankCode = generateBankCode(config);
        String accountNumber = generateAccountNumberPart(config);
        String controlKey = generateControlKey(config);
        
        //  standard sans code agence (sera ajouté par Account Service)
        return bankCode + accountNumber + controlKey;
    }
    
    private String generateBankCode(MicrofinanceConfiguration config) {
        int length = config.getBankCodeLength() != null ? config.getBankCodeLength() : 5;
        if (config.isAffiliatedToBank() && config.getAffiliatedBankCode() != null) {
            return padCode(config.getAffiliatedBankCode(), length);
        }
        return padCode(config.getMicrofinanceCode(), length);
    }
    
    private String generateAccountNumberPart(MicrofinanceConfiguration config) {
        int length = config.getAccountNumberLength() != null ? config.getAccountNumberLength() : 11;
        String strategy = config.getGenerationStrategy() != null ? config.getGenerationStrategy() : "SEQUENTIAL";
        
        switch (strategy) {
            case "SEQUENTIAL":
                return generateSequential(length);
            case "RANDOM":
                return generateRandom(length);
            case "TIMESTAMP":
                return generateTimestamp(length);
            default:
                return generateSequential(length);
        }
    }
    
    private String generateControlKey(MicrofinanceConfiguration config) {
        int length = config.getControlKeyLength() != null ? config.getControlKeyLength() : 2;
        return generateRandom(length);
    }
    
    private String generateSequential(int length) {
        long next = sequentialCounter.getAndIncrement();
        String sequential = String.format("%0" + length + "d", next);
        return sequential.length() > length ? sequential.substring(0, length) : sequential;
    }
    
    private String generateRandom(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    private String generateTimestamp(int length) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        if (timestamp.length() > length) {
            return timestamp.substring(timestamp.length() - length);
        }
        return String.format("%0" + length + "d", Long.parseLong(timestamp));
    }
    
    private String padCode(String code, int targetLength) {
        if (code == null) {
            return generateRandom(targetLength);
        }
        if (code.length() >= targetLength) {
            return code.substring(0, targetLength);
        }
        return String.format("%0" + targetLength + "d", Long.parseLong(code));
    }
    
    private String getShortClientId(String clientId, Integer maxLength) {
        int length = maxLength != null ? maxLength : 11;
        if (clientId == null) return "";
        if (clientId.length() <= length) return clientId;
        return clientId.substring(0, length);
    }
    
    private String formatAccountNumber(String accountNumber, MicrofinanceConfiguration config, String agencyCode) {
        if (accountNumber == null) return null;
        
        int agencyLen = agencyCode != null ? agencyCode.length() : 5;
        String paddedAgencyCode = padCode(agencyCode, agencyLen);
        
        if (config.isUseCustomFormat() && config.getSeparator() != null) {
            String separator = config.getSeparator();
            int bankLen = config.getBankCodeLength() != null ? config.getBankCodeLength() : 5;
            int accountLen = config.getAccountNumberLength() != null ? config.getAccountNumberLength() : 11;
            int keyLen = config.getControlKeyLength() != null ? config.getControlKeyLength() : 2;
            
            if (accountNumber.length() >= bankLen + accountLen + keyLen) {
                return String.format("%s%s%s%s%s%s%s",
                    accountNumber.substring(0, bankLen), separator,
                    paddedAgencyCode, separator,
                    accountNumber.substring(bankLen, bankLen + accountLen), separator,
                    accountNumber.substring(bankLen + accountLen, bankLen + accountLen + keyLen)
                );
            }
        }
        
        //  standard: BANQUE-AGENCE-COMPTE-CLE
        if (accountNumber.length() >= 5 + 11 + 2) {
            return String.format("%s-%s-%s-%s",
                accountNumber.substring(0, 5),
                paddedAgencyCode,
                accountNumber.substring(5, 16),
                accountNumber.substring(16, 18)
            );
        }
        
        return accountNumber;
    }
    
    // 
    //  PRIVÉES DE GÉNÉRATION D'ID CLIENT
    // 
    
    private String generateRandomClientId() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    private String generateSequentialClientId() {
        return String.format("%011d", sequentialCounter.getAndIncrement());
    }
    
    private String generateCustomClientId(String clientEmail, MicrofinanceConfiguration config) {
        String pattern = config.getCustomClientIdPattern();
        if (pattern == null || pattern.isEmpty()) {
            return generateRandomClientId();
        }
        
        String result = pattern
                .replace("{DATE}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .replace("{YEAR}", String.valueOf(LocalDateTime.now().getYear()))
                .replace("{MONTH}", String.format("%02d", LocalDateTime.now().getMonthValue()))
                .replace("{DAY}", String.format("%02d", LocalDateTime.now().getDayOfMonth()))
                .replace("{SEQ}", String.format("%06d", sequentialCounter.getAndIncrement()));
        
        while (result.length() < 11) {
            result += new SecureRandom().nextInt(10);
        }
        return result.length() > 11 ? result.substring(0, 11) : result;
    }
    
    private String generateMixedClientId(MicrofinanceConfiguration config) {
        return config.getMicrofinanceCode() + String.format("%06d", sequentialCounter.getAndIncrement());
    }

   @Transactional
public MicrofinanceConfigurationResponse updateConfiguration(String id, MicrofinanceConfigurationRequest request, String updatedBy) {
    log.info("à jour de la configuration microfinance avec ID: {}", id);
    
    //  la configuration existante
    MicrofinanceConfiguration config = configRepository.findById(id)
            .orElseThrow(() -> new ConfigurationNotFoundException("Configuration non trouvée avec l'ID: " + id));
    
    String newMicrofinanceCode = request.getMicrofinanceCode();
    if (newMicrofinanceCode != null && !newMicrofinanceCode.isEmpty()) {
        //  si le nouveau code n'est pas déjà utilisé par une autre configuration
        if (!config.getMicrofinanceCode().equals(newMicrofinanceCode)) {
            boolean codeExists = configRepository.existsByMicrofinanceCode(newMicrofinanceCode);
            if (codeExists) {
                throw new RuntimeException("Le code microfinance '" + newMicrofinanceCode + "' est déjà utilisé par une autre configuration");
            }
            config.setMicrofinanceCode(newMicrofinanceCode);
        }
    }
    
    // . Mettre à jour les autres champs
    config.setAffiliatedBankCode(request.getAffiliatedBankCode());
    config.setAffiliatedToBank(request.isAffiliatedToBank());
    
    ClientIdGenerationStrategy strategy = request.getClientIdStrategy();
    if (strategy == null) {
        strategy = ClientIdGenerationStrategy.SEQUENTIAL;
    }
    config.setClientIdStrategy(strategy);
    
    config.setCustomClientIdPattern(request.getCustomClientIdPattern());
    config.setEnableRibGeneration(request.isEnableRibGeneration());
    
    //  . Calculer le bankCode FINAL (celui qui sera utilisé pour les RIB)
    String finalBankCode;
    if (request.isAffiliatedToBank() && request.getAffiliatedBankCode() != null && !request.getAffiliatedBankCode().isEmpty()) {
        finalBankCode = request.getAffiliatedBankCode();
    } else {
        //  le NOUVEAU microfinanceCode
        finalBankCode = config.getMicrofinanceCode(); // ←  c'est le nouveau code
    }
    config.setBankCode(finalBankCode);
    
    config.setCountryCode(request.getCountryCode());
    config.setControlKeyAlgorithm(request.getControlKeyAlgorithm());
    config.setUpdatedBy(updatedBy);
    config.setUpdatedAt(LocalDateTime.now());
    
    config = configRepository.save(config);
    log.info("microfinance mise à jour: microfinanceCode={}, bankCode={}", 
        config.getMicrofinanceCode(), config.getBankCode());
    
    return mapToResponse(config);
}
    
    //   Récupérer une configuration par ID
    @Transactional(readOnly = true)
    public MicrofinanceConfigurationResponse getConfigurationById(String id) {
        log.info("de la configuration microfinance avec ID: {}", id);
        
        MicrofinanceConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration non trouvée avec l'ID: " + id));
        
        return mapToResponse(config);
    }
    
    //   Récupérer toutes les configurations
    @Transactional(readOnly = true)
    public List<MicrofinanceConfigurationResponse> getAllConfigurations() {
        log.info("de toutes les configurations microfinance");
        
        return configRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    //   Activer/Désactiver une configuration
    @Transactional
    public MicrofinanceConfigurationResponse toggleConfigurationActive(String id) {
        log.info("/désactivation de la configuration microfinance avec ID: {}", id);
        
        MicrofinanceConfiguration config = configRepository.findById(id)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration non trouvée avec l'ID: " + id));
        
        if (!config.isActive()) {
            //  on active cette configuration, désactiver les autres
            configRepository.findByActiveTrue().ifPresent(activeConfig -> {
                if (!activeConfig.getId().equals(id)) {
                    activeConfig.setActive(false);
                    configRepository.save(activeConfig);
                }
            });
        }
        
        config.setActive(!config.isActive());
        config.setUpdatedAt(LocalDateTime.now());
        config = configRepository.save(config);
        
        log.info("{} activée: {}", config.getMicrofinanceCode(), config.isActive());
        
        return mapToResponse(config);
    }
    
    private MicrofinanceConfigurationResponse mapToResponse(MicrofinanceConfiguration config) {
    return MicrofinanceConfigurationResponse.builder()
            .id(config.getId())
            .microfinanceCode(config.getMicrofinanceCode())
            .affiliatedBankCode(config.getAffiliatedBankCode())
            .affiliatedToBank(config.isAffiliatedToBank())
            .clientIdStrategy(config.getClientIdStrategy())  //  l'enum
            .customClientIdPattern(config.getCustomClientIdPattern())
            .enableRibGeneration(config.isEnableRibGeneration())
            .bankCode(config.getBankCode())
            .countryCode(config.getCountryCode())
            .controlKeyAlgorithm(config.getControlKeyAlgorithm())
            .useCustomFormat(config.isUseCustomFormat())
            .accountNumberFormat(config.getAccountNumberFormat())
            .bankCodeLength(config.getBankCodeLength())
            .accountNumberLength(config.getAccountNumberLength())
            .controlKeyLength(config.getControlKeyLength())
            .separator(config.getSeparator())
            .fixedPrefix(config.getFixedPrefix())
            .fixedSuffix(config.getFixedSuffix())
            .generationStrategy(config.getGenerationStrategy())
            .includeCheckDigit(config.isIncludeCheckDigit())
            .active(config.isActive())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
}
    
    // 
    //  RIB
    // 
    
    @Transactional(readOnly = true)
    public String generateRib(String accountNumber, String clientId) {
        log.info("du RIB pour le compte: {}, client: {}", accountNumber, clientId);
        return ribGeneratorService.generateRib(accountNumber, clientId);
    }
    
    @Transactional(readOnly = true)
    public String formatRib(String rib) {
        return ribGeneratorService.formatRib(rib);
    }
    
    @Transactional(readOnly = true)
    public boolean validateRib(String rib) {
        return ribGeneratorService.validateRib(rib);
    }
    
    @Transactional(readOnly = true)
    public String generateFullAccountNumber(String clientId) {
        String accountNumber = generateAccountNumber(clientId);
        String rib = ribGeneratorService.generateRib(accountNumber, clientId);
        log.info("de compte généré: {}, RIB: {}", accountNumber, rib);
        return rib;
    }
     

    
}