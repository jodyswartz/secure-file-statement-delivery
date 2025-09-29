# -------- Build stage --------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -U -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre
RUN useradd -u 10001 appuser
WORKDIR /opt/app
COPY --from=build /app/target/secure-file-statement-delivery-*.jar app.jar
USER appuser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=""
HEALTHCHECK --interval=30s --timeout=3s CMD curl -fsS http://localhost:8080/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/opt/app/app.jar"]
