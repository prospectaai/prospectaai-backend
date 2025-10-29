FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o POM pai e todos os módulos
COPY ./pom.xml ./pom.xml
COPY ./shared ./shared
COPY ./ms-auto-kafka-topic ./ms-auto-kafka-topic
COPY ./ms-billing-sbs ./ms-billing-sbs
COPY ./ms-useraccount ./ms-useraccount

# Build executável de todos os módulos via POM pai
RUN mvn clean package -DskipTests -f ./pom.xml

# Copia apenas o jar final do ms-auto-kafka-topic
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ms-auto-kafka-topic/target/*.jar app.jar
EXPOSE 9000
CMD ["java", "-jar", "app.jar"]
