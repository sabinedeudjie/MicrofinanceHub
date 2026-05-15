package org.example.agencyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import org.example.agencyservice.dto.response.ClientInfo;

import java.util.List;

@FeignClient(name = "CLIENT-SERVICE", url = "${client.service.url:http://localhost:8081}")
public interface ClientServiceClient {

    @PostMapping("/api/clients/internal/by-agent-emails")
    List<ClientInfo> getClientsByAgentEmails(@RequestBody List<String> agentEmails);

    @GetMapping("/api/clients/{clientId}")
    ClientInfo getClientInfo(@PathVariable("clientId") String clientId,
                             @RequestHeader("Authorization") String token);

    @GetMapping("/api/clients/by-email-exact")
    ClientInfo getClientByEmailExact(@RequestParam("email") String email,
                                     @RequestHeader("Authorization") String token);

    @GetMapping("/api/clients/by-agency/{agencyId}")
    List<ClientInfo> getClientsByAgency(@PathVariable("agencyId") String agencyId,
                                        @RequestHeader("Authorization") String token);
}