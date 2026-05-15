package org.example.reportingservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import org.example.reportingservice.config.FeignClientConfig;
import org.example.reportingservice.dto.ClientInfo;

@FeignClient(name = "CLIENT-SERVICE",
            url = "${client.service.url:http://localhost:8081}",
            configuration = FeignClientConfig.class)
public interface ClientServiceClient {

    
    @GetMapping("/api/clients/stats")
    ClientStats getClientStats(@RequestHeader("Authorization") String token);

    
    @GetMapping("/api/clients/by-agent/{agentId}")
    List<ClientInfo> getClientsByAgent(@PathVariable("agentId") String agentId,
                                        @RequestHeader("Authorization") String token);
    
    
    @GetMapping("/api/clients/{clientId}")
    ClientInfo getClientInfo(@PathVariable("clientId") String clientId,
                             @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/clients/stats/by-clients")
    ClientStats getClientStatsForClients(@RequestBody List<String> clientIds,
                                          @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/clients/by-email")
    ClientInfo getClientInfoByEmail(@RequestParam("email") String email,
                                     @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/clients/stats/by-agent/{agentId}")
    ClientStats getClientStatsForAgent(@PathVariable("agentId") String agentId,
                                        @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/clients/by-agent/{agentId}/client-ids")
    List<String> getClientIdsByAgent(@PathVariable("agentId") String agentId,
                                      @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/clients/my-clients")
    List<ClientInfo> getMyClients(@RequestHeader("X-User-Id") String agentId,
                                   @RequestHeader("Authorization") String token);
    
    
    @GetMapping("/api/clients/my-clients/count")
    Long countMyClients(@RequestHeader("X-User-Id") String agentId,
                         @RequestHeader("Authorization") String token);
    
    
    @GetMapping("/api/clients/my-clients/stats")
    ClientStats getMyClientsStats(@RequestHeader("X-User-Id") String agentId,
                                   @RequestHeader("Authorization") String token);
}