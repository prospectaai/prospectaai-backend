// Classe SubscriptionValidationFilter
package br.com.prospectaai.service_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class SubscriptionValidationFilter extends AbstractGatewayFilterFactory<SubscriptionValidationFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final RouteValidator validator;

    @Value("${useraccount.service.base-url:lb://ms-useraccount}")
    private String userAccountBaseUrl;

    @Value("${billing.service.base-url:lb://ms-billing-sbs}")
    private String billingBaseUrl;

    public SubscriptionValidationFilter(WebClient.Builder webClientBuilder, RouteValidator validator) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            boolean secured = validator.isSecured.test(exchange.getRequest());
            System.out.println("[Gateway][SubFilter] request path=" + exchange.getRequest().getURI().getPath() + ", method=" + exchange.getRequest().getMethod() + ", secured=" + secured);
            if (secured) {
                if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    System.out.println("[Gateway][SubFilter] skipping OPTIONS");
                    return chain.filter(exchange);
                }
                String email = exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id");
                if (email == null || email.isBlank()) {
                    System.out.println("[Gateway][SubFilter] missing X-Auth-User-Id");
                    return onError(exchange, "Missing X-Auth-User-Id", HttpStatus.UNAUTHORIZED);
                }

                System.out.println("[Gateway][SubFilter] resolving userId for email=" + email + " via " + userAccountBaseUrl + "/api/v1/users/resolve-id");
                return webClientBuilder.build()
                        .get()
                        .uri(userAccountBaseUrl + "/api/v1/users/resolve-id?email=" + email)
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(userId -> {
                                System.out.println("[Gateway][SubFilter] resolved userId=" + userId + "; checking subscription via " + billingBaseUrl + "/api/v1/billing/subscriptions/status");
                                return webClientBuilder.build()
                                .get()
                                .uri(billingBaseUrl + "/api/v1/billing/subscriptions/status?userId=" + userId)
                                .retrieve()
                                .bodyToMono(br.com.prospectaai.shared.dto.billing.SubscriptionStatusResponse.class);
                        })
                        .flatMap(res -> {
                            boolean active = (res != null && res.isActive());
                            String plan = (res != null && res.getPlan() != null) ? res.getPlan().name() : "FREE";
                            System.out.println("[Gateway][SubFilter] subscription active=" + active + ", plan=" + plan);
                            
                            if (!active) {
                                return onError(exchange, "Subscription inactive", HttpStatus.PAYMENT_REQUIRED);
                            }
                            
                            ServerWebExchange modifiedExchange = exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                    .header("X-Subscription-Active", "true")
                                    .header("X-Subscription-Plan", plan)
                                    .build())
                                .build();
                                
                            return chain.filter(modifiedExchange);
                        })
                        .onErrorResume(error -> onError(exchange, "Subscription check failed", HttpStatus.BAD_GATEWAY));
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        System.out.println("[Gateway][SubFilter] error: " + message + ", status=" + status);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // placeholder for future configuration
    }
}
