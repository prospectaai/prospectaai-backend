# Guia: Inserção de um novo módulo/microserviço

Este guia descreve, passo a passo, como adicionar um novo módulo Spring Boot ao monorepo `prospectaai-backend`, seguindo o padrão de build via POM pai e a integração com Docker e Docker Compose.

## Pré-requisitos
- Java 21 instalado localmente.
- Docker e Docker Compose instalados.
- Maven 3.9+ (ou uso da imagem `maven:3.9.6-eclipse-temurin-21-alpine`).

## 1) Criar a estrutura do módulo
- Na raiz do projeto, crie a pasta do módulo, por exemplo: `./ms-novo-servico`.
- Inicialize um projeto Spring Boot (via Spring Initializr ou manualmente) dentro dessa pasta.

## 2) Configurar o `pom.xml` do módulo
- Em `./ms-novo-servico/pom.xml`, referencie o POM pai agregador:

```xml
<parent>
  <groupId>br.com.prospectaai</groupId>
  <artifactId>prospectaai-backend</artifactId>
  <version>1.0.0</version>
  <relativePath>../pom.xml</relativePath>
</parent>
```

- Defina `groupId`, `artifactId` e `version` do módulo conforme o padrão do monorepo.
- Não utilize `spring-boot-starter-parent` como `<parent>`; o gerenciamento é feito pelo POM pai.
- Mantenha `java.version` e declare dependências do Spring Boot normalmente (as versões são gerenciadas por `dependencyManagement` do POM pai).
- Se o módulo usar o `shared`, adicione:

```xml
<dependency>
  <groupId>br.com.prospectaai</groupId>
  <artifactId>shared</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 3) Atualizar o POM pai agregador
- No arquivo `./pom.xml` (raiz), inclua o novo módulo na seção `<modules>`:

```xml
<modules>
  ...
  <module>ms-novo-servico</module>
</modules>
```

## 4) Criar o Dockerfile do novo módulo
- Dentro de `./ms-novo-servico/Dockerfile`, use o padrão de build via POM pai:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia o POM pai e todos os módulos
COPY ./pom.xml ./pom.xml
COPY ./shared ./shared
COPY ./service-gateway ./service-gateway
COPY ./service-discovery ./service-discovery
COPY ./ms-useraccount ./ms-useraccount
COPY ./ms-billing-sbs ./ms-billing-sbs
COPY ./ms-auto-kafka-topic ./ms-auto-kafka-topic
COPY ./ms-novo-servico ./ms-novo-servico

# Build de todos os módulos via POM pai
RUN mvn clean install -DskipTests -f ./pom.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ms-novo-servico/target/*.jar app.jar
EXPOSE 808X
CMD ["java", "-jar", "app.jar"]
```

Observações:
- É importante copiar todas as pastas declaradas em `<modules>` no POM pai; caso contrário, o `mvn -f ./pom.xml` falhará com erro de módulo inexistente.
- Ajuste a porta exposta (`EXPOSE`) para a porta do seu serviço.

## 5) Atualizar o `docker-compose.yaml`
- Adicione um novo serviço apontando para o `Dockerfile` do módulo:

```yaml
ms-novo-servico:
  build:
    context: .
    dockerfile: ./ms-novo-servico/Dockerfile
  container_name: ms-novo-servico
  depends_on:
    service-discovery:
      condition: service_healthy
    kafka:
      condition: service_healthy
  environment:
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  ports:
    - "808X:808X"
  networks:
    - prospectaai-network
```

## 6) Build e execução
- Na raiz do projeto:
  - `docker-compose up --build`
- Para build local sem Docker:
  - `mvn clean install -DskipTests` (na raiz)

## 7) Dicas e problemas comuns
- Certifique-se de que todos os módulos listados em `<modules>` do POM pai estão sendo copiados nos Dockerfiles (inclusive `service-gateway` e `service-discovery`).
- No POM de cada módulo, use sempre o POM pai agregador como `<parent>` com `relativePath` correto.
- Evite versões fixas nas dependências do Spring; deixe o `dependencyManagement` do POM pai gerenciar.
- Se o build reclamar de artefatos não encontrados, valide se o `shared` foi compilado e se o POM pai está presente dentro da imagem durante o build.

---

Seguindo estes passos, qualquer novo microserviço será integrado corretamente ao monorepo, ao processo de build via POM pai e ao ambiente de containers.