package br.com.prospectaai.service_gateway.config;

import br.com.prospectaai.service_gateway.filter.AuthenticationFilter;
import br.com.prospectaai.service_gateway.filter.SubscriptionValidationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, AuthenticationFilter authFilter, SubscriptionValidationFilter subscriptionFilter) {
        return builder.routes()
                .route("ms-useraccount", r -> r.path("/api/v1/auth/**", "/api/v1/users/**")
                        .filters(f -> f
                                .filter(authFilter.apply(new AuthenticationFilter.Config()))
                                .filter(subscriptionFilter.apply(new SubscriptionValidationFilter.Config()))
                        )
                        .uri("lb://ms-useraccount"))
                .route("ms-billing-sbs", r -> r.path("/api/v1/billing/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://ms-billing-sbs"))
                .route("ms-async-task", r -> r.path("/api/v1/async/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://ms-async-task"))
                .route("ms-notification", r -> r.path("/api/v1/notification/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://ms-notification"))
                .build();
    }
}
