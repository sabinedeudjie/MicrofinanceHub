package org.example.clientservice.dto.request;

import org.example.clientservice.model.enums.ClientType;
import org.example.clientservice.security.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientRequest {
    
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;
    
    @ValidPhoneNumber
    private String phoneNumber;
    
    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    private String firstName;
    
    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String lastName;
    
    private String address;
    
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDateTime birthDate;
    
    private ClientType clientType;

    private String agencyId;
}

//  org.example.clientservice.dto.request;

//  org.example.clientservice.model.enums.ClientType;
//  jakarta.validation.constraints.Email;
//  jakarta.validation.constraints.NotBlank;
//  jakarta.validation.constraints.Past;
//  lombok.Data;

//  java.time.LocalDateTime;

// 
//  class ClientRequest {
    
//     (message = "L'email est requis")
//     (message = "Format d'email invalide")
//      String email;
    
//      String phoneNumber;
    
//     (message = "Le prénom est requis")
//      String firstName;
    
//     (message = "Le nom est requis")
//      String lastName;
    
//      String address;
    
//     (message = "La date de naissance doit être dans le passé")
//      LocalDateTime birthDate;
    
//      ClientType clientType;
// 