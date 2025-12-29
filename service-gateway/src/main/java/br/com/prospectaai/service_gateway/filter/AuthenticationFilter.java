package br.com.prospectaai.service_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final RouteValidator validator;

    @Value("${useraccount.service.base-url:lb://ms-useraccount}")
    private String userAccountBaseUrl;

    public AuthenticationFilter(WebClient.Builder webClientBuilder, RouteValidator validator) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            boolean secured = validator.isSecured.test(exchange.getRequest());
            if (secured) {
                if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    return chain.filter(exchange);
                }
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                Mono<String> emailMono = webClientBuilder.build()
                        .get()
                        .uri(userAccountBaseUrl + "/api/v1/auth/validate")
                        .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(error -> {
                            if (error instanceof WebClientResponseException wcre) {
                                HttpStatus status = HttpStatus.resolve(wcre.getRawStatusCode());
                                if (status == HttpStatus.UNAUTHORIZED) {
                                    return Mono.error(new IllegalArgumentException("unauthorized"));
                                }
                                return Mono.error(new IllegalStateException("auth_upstream_" + wcre.getRawStatusCode()));
                            }
                            return Mono.error(new IllegalStateException("auth_upstream_error"));
                        });

                return emailMono
                        .flatMap(response -> {
                            String email = response == null ? null : response.trim();
                            if (email == null || email.isBlank() || !isValidEmail(email)) {
                                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                            }
                            ServerWebExchange mutated = exchange.mutate()
                                    .request(
                                        exchange.getRequest()
                                            .mutate()
                                            .header("X-Auth-User-Id", email)
                                            .build()
                                    )
                                    .build();
                            return chain.filter(mutated);
                        })
                        .onErrorResume(IllegalArgumentException.class, e ->
                                onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED)
                        )
                        .onErrorResume(IllegalStateException.class, e ->
                                onError(exchange, "Auth validation failed", HttpStatus.BAD_GATEWAY)
                        );
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        System.out.println("[Gateway][AuthFilter] error: " + message + ", status=" + httpStatus);
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static class Config {
        // Configuration properties if needed
    }
}
