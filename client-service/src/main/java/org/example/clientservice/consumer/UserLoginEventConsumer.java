package org.example.clientservice.consumer;

import org.example.clientservice.model.Client;
import org.example.clientservice.repository.ClientRepository;
// org.example.clientservice.service.ClientService;
import org.example.commonservice.event.UserLoginEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginEventConsumer {

    // final ClientService clientService;
    private final ClientRepository clientRepository;

    @RabbitListener(queues = "${rabbitmq.queue.user-login}")
    public void handleUserLogin(UserLoginEvent event) {
        log.info("de l'événement de login pour: {}", event.getEmail());
        
        try {
            //  si le client existe avant de mettre à jour
            Optional<Client> clientOpt = clientRepository.findByEmail(event.getEmail());
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                client.setLastLoginAt(event.getTimestamp());
                clientRepository.save(client);
                log.info("connexion mise à jour pour: {}", event.getEmail());
            } else {
                log.warn("non trouvé pour l'email: {}, mise à jour ignorée", event.getEmail());
            }
        } catch (Exception e) {
            log.error("lors de la mise à jour pour {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}