package br.com.prospectaai.service_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final RouteValidator validator;

    public AuthenticationFilter(WebClient.Builder webClientBuilder, RouteValidator validator) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                return webClientBuilder.build()
                        .get()
                        .uri("lb://ms-useraccount/api/v1/auth/validate?token=" + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(response -> {
                            exchange.getRequest()
                                    .mutate()
                                    .header("X-Auth-User-Id", response);
                            return exchange;
                        })
                        .flatMap(chain::filter)
                        .onErrorResume(error -> onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED));
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}