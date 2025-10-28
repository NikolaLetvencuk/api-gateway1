package com.api_gateway1.api_gateway1.controller;

import com.api_gateway1.api_gateway1.grpc.StakeholdersGrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux REST Controller koji koristi gRPC klijent
 * 
 * Gateway prima HTTP zahtev i interno poziva Stakeholders preko gRPC-a
 */
@RestController
public class GrpcTestController {

    private final StakeholdersGrpcClient grpcClient;

    @Autowired
    public GrpcTestController(StakeholdersGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    /**
     * Test endpoint: Gateway → gRPC → Stakeholders
     * 
     * HTTP GET: http://localhost:8080/grpc/user/check?username=ratkovac
     */
    @GetMapping("/grpc/user/check")
    public Mono<Map<String, Object>> checkUser(@RequestParam String username) {
        System.out.println("📥 Gateway: HTTP zahtev primljen za proveru korisnika: " + username);

        return Mono.fromCallable(() -> {
            // Pozivamo gRPC metode (blokirajuće, ali u Mono.fromCallable se izvršava async)
            boolean exists = grpcClient.checkUserExists(username);
            boolean blocked = grpcClient.isUserBlocked(username);
            String role = grpcClient.getUserRole(username);

            // Kreiramo odgovor
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("exists", exists);
            response.put("blocked", blocked);
            response.put("role", role);
            response.put("message", exists ? "Korisnik pronađen preko gRPC-a!" : "Korisnik ne postoji!");

            System.out.println("📤 Gateway: HTTP odgovor poslan");
            return response;
        });
    }
}

