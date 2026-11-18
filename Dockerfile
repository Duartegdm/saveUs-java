# Uma imagem por serviço, a partir do mesmo Dockerfile:
#   docker build --build-arg SERVICO=refugio-service -t saveus/refugio-service .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY saveus-common saveus-common
COPY eureka-server eureka-server
COPY refugio-service refugio-service
COPY ia-service ia-service
ARG SERVICO
RUN chmod +x gradlew && ./gradlew :${SERVICO}:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
ARG SERVICO
WORKDIR /app
RUN useradd --system --uid 10001 saveus
COPY --from=build /app/${SERVICO}/build/libs/${SERVICO}-*.jar app.jar
USER saveus
ENV TZ=America/Sao_Paulo
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
