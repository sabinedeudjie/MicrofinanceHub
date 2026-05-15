# ─── Étape 1 : Build ────────────────────────────────────────────────────────
# Le contexte de build doit être la RACINE du projet (pour accéder à common-service)
# docker build -f auth-service/Dockerfile .
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Installer common-service en premier (dépendance locale)
COPY common-service/.mvn/ common-service/.mvn/
COPY common-service/mvnw common-service/pom.xml common-service/
RUN chmod +x common-service/mvnw && \
    cd common-service && ./mvnw dependency:go-offline -q
COPY common-service/src/ common-service/src/
RUN cd common-service && ./mvnw install -DskipTests -q

# 2. Builder auth-service
COPY auth-service/.mvn/ auth-service/.mvn/
COPY auth-service/mvnw auth-service/pom.xml auth-service/
RUN chmod +x auth-service/mvnw && \
    cd auth-service && ./mvnw dependency:go-offline -q
COPY auth-service/src/ auth-service/src/
RUN cd auth-service && ./mvnw package -DskipTests -q

# ─── Étape 2 : Image finale légère ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/auth-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
