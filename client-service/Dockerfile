# ─── Étape 1 : Build ────────────────────────────────────────────────────────
# Le contexte de build doit être la RACINE du projet (pour accéder à common-service)
# docker build -f client-service/Dockerfile .
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Installer common-service en premier (dépendance locale)
COPY common-service/.mvn/ common-service/.mvn/
COPY common-service/mvnw common-service/pom.xml common-service/
RUN chmod +x common-service/mvnw && \
    cd common-service && ./mvnw dependency:go-offline -q
COPY common-service/src/ common-service/src/
RUN cd common-service && ./mvnw install -DskipTests -q

# 2. Builder client-service
COPY client-service/.mvn/ client-service/.mvn/
COPY client-service/mvnw client-service/pom.xml client-service/
RUN chmod +x client-service/mvnw && \
    cd client-service && ./mvnw dependency:go-offline -q
COPY client-service/src/ client-service/src/
RUN cd client-service && ./mvnw package -DskipTests -q

# ─── Étape 2 : Image finale légère ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/client-service/target/*.jar app.jar

# Dossier pour les uploads de documents KYC (monté en volume dans docker-compose)
RUN mkdir -p /app/uploads

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
