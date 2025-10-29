package com.api_gateway1.api_gateway1.filter;

import com.api_gateway1.api_gateway1.dto.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private final String stakeholdersServiceUrl;

    public AuthenticationFilter() {
        // Koristi environment varijablu (Docker: service-stakeholders:8081, Lokalno: localhost:8081)
        this.stakeholdersServiceUrl = System.getenv("STAKEHOLDERS_SERVICE_URL") != null 
            ? System.getenv("STAKEHOLDERS_SERVICE_URL") 
            : "http://localhost:8081";
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        final List<String> publicEndpoints = List.of("/auth/register", "/auth/login", "/uploads/profile-pictures");
        String path = request.getURI().getPath();
        boolean isPublic = publicEndpoints.stream().anyMatch(uri -> path.contains(uri));
        if (isPublic) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            System.err.println("!!! Authorization header nije prisutan u zahtevu!!!");
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
        final String authHeader = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0);

        return webClientBuilder.build().get()
                .uri(stakeholdersServiceUrl + "/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .flatMap(response -> {

                    System.out.println("--- API Gateway: Primljen odgovor od /auth/validate ---");
                    if (response != null) {
                        System.out.println("Username: " + response.getUsername());
                        System.out.println("Role: " + response.getRole());
                        System.out.println("IsValid: " + response.isValid());
                    } else {
                        System.out.println("Odgovor (response) je NULL!");
                    }
                    System.out.println("------------------------------------------------------");
                    System.out.println("➕ Gateway dodaje header X-Username: " + response.getUsername());
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-Username", response.getUsername())
                            .header("X-User-Role", response.getRole())
                            .build();
                    System.out.println("➡️ Prosleđujem request dalje na: " + path);
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    System.err.println("!!! Greška prilikom poziva /auth/validate endpointa: " + e.getMessage());
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

