package org.example.loanservice.client;

import org.example.loanservice.client.model.ClientInfo;
import org.example.loanservice.config.FeignClientConfig;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "CLIENT-SERVICE",
              url = "${client.service.url:http://localhost:8081}",
             configuration = FeignClientConfig.class)
public interface ClientServiceClient {
    
    @GetMapping("/api/clients/{id}/credit-score/value")
    Integer getClientCreditScore(@PathVariable("id") String clientId,
                                 @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/clients/{id}")
    ClientInfo getClientInfo(@PathVariable("id") String clientId, 
                             @RequestHeader("Authorization") String token);

     @GetMapping("/api/clients/exists/by-email")
    Boolean clientExistsByEmail(@RequestParam("email") String email,
                                 @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/clients/{id}/exists")
    Boolean clientExists(@PathVariable("id") String clientId,
                         @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/clients/by-email-exact")
    ClientInfo getClientByEmail(@RequestParam("email") String email,
                                 @RequestHeader("Authorization") String token);
}
