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
        "/api/v1/auth/login",
        "/api/v1/auth/oauth2/**",
        "/eureka/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public Predicate<ServerHttpRequest> isSecured =
        request -> openApiEndpoints.stream()
            .noneMatch(uri -> pathMatcher.match(uri, request.getURI().getPath()));
}
