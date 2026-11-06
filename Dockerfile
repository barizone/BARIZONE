# syntax=docker/dockerfile:1
# ── Build: backend + frontend in un unico JAR ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -Dskip.frontend=true
COPY . .
RUN mvn -B -DskipTests package

# ── Runtime: solo JRE, utente non privilegiato ──
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 barizone
WORKDIR /app
COPY --from=build /src/target/barizone.jar app.jar
USER barizone
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Health check: GET /actuator/health/readiness (configuralo nella piattaforma o nel proxy).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
