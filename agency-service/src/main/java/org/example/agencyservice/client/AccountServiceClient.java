package org.example.agencyservice.client;

import org.example.agencyservice.config.FeignClientConfig;
import org.example.agencyservice.dto.response.AccountResponse;
import org.example.agencyservice.dto.response.AgencyAccountInfo;
import org.example.agencyservice.dto.response.AgencyStatsResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "ACCOUNT-SERVICE", url = "${account.service.url:http://localhost:8082}", configuration = FeignClientConfig.class)
public interface AccountServiceClient {
    
    @GetMapping("/api/internal/accounts/client/{clientId}/all")
    List<AgencyAccountInfo> getClientAccounts(@PathVariable("clientId") String clientId,
                                               @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/client/{clientId}/exists")
    Boolean clientHasAccounts(@PathVariable("clientId") String clientId,
                               @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/client/{clientId}/total-balance")
    BigDecimal getClientTotalBalance(@PathVariable("clientId") String clientId,
                                      @RequestHeader("Authorization") String token);

    @GetMapping("/api/accounts/by-agency/{agencyId}")
    List<AccountResponse> getAccountsByAgency(@PathVariable("agencyId") String agencyId,
                                               @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/accounts/internal/stats/by-agency/{agencyId}")
    AgencyStatsResponse getAgencyAccountStats(@PathVariable("agencyId") String agencyId);

    @GetMapping("/api/accounts/internal/by-agency/{agencyId}")
    List<AccountResponse> getAccountsByAgency(@PathVariable("agencyId") String agencyId);

    @GetMapping("/api/accounts/internal/by-agency/{agencyId}")
    List<AccountResponse> getAccountsByAgencyInternal(@PathVariable("agencyId") String agencyId);

    @GetMapping("/api/accounts/client/{clientId}")
    List<AccountResponse> getAccountsByClientId(@PathVariable("clientId") String clientId,
                                                 @RequestHeader("Authorization") String token);

}