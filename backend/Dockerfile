# syntax=docker/dockerfile:1
# Image du backend. Contexte de build = racine du dépôt (le contrat api/contrat.yaml est copié dans le jar).
#   docker compose build                                   → images depuis Docker Hub
#   docker compose build --build-arg REGISTRY=localhost:8082 → images via le proxy Nexus (docs/INFRA.md)

ARG REGISTRY=docker.io

# ── Étape 1 : build Maven ────────────────────────────────────────────────────
FROM ${REGISTRY}/library/maven:3.9-eclipse-temurin-17 AS build
# Miroir Maven optionnel (Nexus) ; vide = Maven Central
ARG MAVEN_MIRROR_URL=
# Commit affiché par /actuator/info (le dossier .git n'est pas copié dans l'image)
ARG GIT_COMMIT=inconnu
WORKDIR /workspace

RUN if [ -n "$MAVEN_MIRROR_URL" ]; then \
      mkdir -p /root/.m2 && printf '<settings><mirrors><mirror><id>miroir</id><mirrorOf>*</mirrorOf><url>%s</url></mirror></mirrors></settings>' "$MAVEN_MIRROR_URL" > /root/.m2/settings-miroir.xml; \
    fi

COPY api/contrat.yaml api/contrat.yaml
COPY backend/pom.xml backend/pom.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    cd backend && mvn -B -ntp -q $([ -f /root/.m2/settings-miroir.xml ] && echo "-s /root/.m2/settings-miroir.xml") dependency:go-offline

COPY backend/src backend/src
RUN printf 'git.commit.id.abbrev=%s\n' "$GIT_COMMIT" > backend/src/main/resources/git.properties
RUN --mount=type=cache,target=/root/.m2/repository \
    cd backend && mvn -B -ntp -q $([ -f /root/.m2/settings-miroir.xml ] && echo "-s /root/.m2/settings-miroir.xml") package -DskipTests -Dmaven.gitcommitid.skip=true \
    && cp target/presence-*.jar /workspace/app.jar

# ── Étape 2 : exécution ──────────────────────────────────────────────────────
FROM ${REGISTRY}/library/eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system presence && useradd --system --gid presence --no-create-home presence
COPY --from=build --chown=presence:presence /workspace/app.jar app.jar
USER presence

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Sonde sans curl : requête HTTP brute sur la readiness d'Actuator
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
