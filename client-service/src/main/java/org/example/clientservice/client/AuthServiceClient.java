package org.example.clientservice.client;

import org.example.clientservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "AUTH-SERVICE",
             url = "${auth.service.url:http://localhost:8080}")
public interface AuthServiceClient {
    
    @GetMapping("/auth/users/{id}")
    UserDTO getUserById(@PathVariable("id") String id,
                        @RequestHeader("Authorization") String token);
    
    @GetMapping("/auth/users/by-email")
    UserDTO getUserByEmail(@RequestParam("email") String email,
                           @RequestHeader("Authorization") String token);
}