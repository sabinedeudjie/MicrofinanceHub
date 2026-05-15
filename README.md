# MicrofinanceHub : Système de Gestion de Microfinance

MicrofinanceHub est une plateforme de gestion de microfinance basée sur une architecture microservices. Elle permet de gérer les clients, les comptes bancaires et transactions, les prêts, les remboursements et les notifications automatisées.

## Architecture Technique
- **Backend :** Java 21, Spring Boot 3, Spring Cloud (Config, Registry, Gateway)
- **Frontend :** React.js, Tailwind CSS, Lucide React
- **Base de données :** PostgreSQL (10 bases distinctes)
- **Message Broker :** RabbitMQ (Architecture orientée événements)
- **Cache :** Redis
- **Conteneurisation :** Docker & Docker Compose

## Prérequis
Vous avez uniquement besoin de :
- [Docker] installé et démarré.
- [Git] pour cloner le projet.

## Installation et Lancement

1. **Cloner le projet :**
   ```bash
   git clone https://github.com/sabinedeudjie/MicrofinanceHub.git
   cd MicrofinanceHub
   ```

2. **Configuration de l'environnement :**
   Copiez le fichier d'exemple et créez votre fichier `.env` :

   *(Note : remplacez les valeurs par défaut par les vôtres).*

3. **Démarrer l'application :**
   ```bash
   docker compose up -d --build
   ```

## Accès à l'application

Une fois que tous les conteneurs sont démarrés :
- **Frontend :** [http://localhost:3000](http://localhost:3000)
- **Identifiants Admin par défaut :**
    - **Email :** `admin@mfh.com`
    - **Mot de passe :** `Admin123!`

## Outils de Monitoring (Dev)
- **Eureka (Registry) :** [http://localhost:8761](http://localhost:8761)
- **RabbitMQ Management :** [http://localhost:15672](http://localhost:15672)
- **Config Server :** [http://localhost:8000](http://localhost:8000)

## Fonctionnalités Clés
- **Gestion Client :** KYC, documents, profils.
- **Comptes :** Création, validation par le directeur, dépôts/retraits (intégré avec CamPay).
- **Prêts :** Simulation, demande, approbation multi-niveaux, décaissement.
- **Remboursements :** Gestion des échéances, paiements partiels/totaux.
- **Notifications :** Emails automatiques pour chaque opération critique.
