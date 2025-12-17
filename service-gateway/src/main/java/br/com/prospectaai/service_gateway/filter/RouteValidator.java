package br.com.prospectaai.service_gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final List<String> openApiEndpoints = List.of(
        "/api/v1/auth/register",
        "/api/v1/auth/confirm-code",
        "/api/v1/auth/resend-code",
        "/api/v1/auth/validate-pre-register-id",
        "/api/v1/auth/checkout",
        "/api/v1/auth/login",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/reset-password/validate",
        "/api/v1/auth/oauth2/**",
        "/api/v1/auth/login/oauth2/**",
        "/api/v1/billing/charge",
        "/eureka/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public Predicate<ServerHttpRequest> isSecured =
        request -> openApiEndpoints.stream()
            .noneMatch(uri -> pathMatcher.match(uri, request.getURI().getPath()));
}
