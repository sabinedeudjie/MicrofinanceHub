package org.example.loanservice.client;

import org.example.loanservice.client.model.AccountInfo;
import org.example.loanservice.config.FeignClientConfig;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ACCOUNT-SERVICE",
              url = "${account.service.url:http://localhost:8082}",
              configuration = FeignClientConfig.class)
public interface AccountServiceClient {
    
    @GetMapping("/api/internal/accounts/client/{clientId}/exists")
    Boolean accountExists(@PathVariable("clientId") String clientId, 
                          @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/client/{clientId}/status")
    String getAccountStatus(@PathVariable("clientId") String clientId,
                            @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/number/{accountNumber}")
    AccountInfo getAccountByNumber(@PathVariable("accountNumber") String accountNumber,
                                   @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/client/{clientId}/all")
    List<AccountInfo> getAccountsByClientId(@PathVariable("clientId") String clientId,
                                            @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/accounts/validate/{accountNumber}/client/{clientId}")
    Boolean validateAccountOwnership(@PathVariable("accountNumber") String accountNumber,
                                     @PathVariable("clientId") String clientId,
                                     @RequestHeader("Authorization") String token);
}