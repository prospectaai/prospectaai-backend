FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o POM pai e todos os módulos
COPY ./pom.xml ./pom.xml
COPY ./shared ./shared
COPY ./service-gateway ./service-gateway
COPY ./service-discovery ./service-discovery
COPY ./ms-auto-kafka-topic ./ms-auto-kafka-topic
COPY ./ms-billing-sbs ./ms-billing-sbs
COPY ./ms-useraccount ./ms-useraccount
COPY ./ms-email ./ms-email
COPY ./sdk/prospection-sdk ./sdk/prospection-sdk
COPY ./ms-async-task ./ms-async-task
COPY ./ms-notification ./ms-notification

# Build executável de todos os módulos via POM pai
RUN mvn clean install -DskipTests -f ./pom.xml

# Copia apenas o jar final do ms-auto-kafka-topic
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ms-auto-kafka-topic/target/*SNAPSHOT.jar app.jar
EXPOSE 9000
CMD ["java", "-jar", "app.jar"]
