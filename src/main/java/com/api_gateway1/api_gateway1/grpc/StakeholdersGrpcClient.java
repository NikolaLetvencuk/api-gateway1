package com.api_gateway1.api_gateway1.grpc;

import com.stakeholders.grpc.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class StakeholdersGrpcClient {

    @Value("${grpc.stakeholders.host:localhost}")
    private String host;

    @Value("${grpc.stakeholders.port:9091}")
    private int port;

    private ManagedChannel channel;
    private StakeholdersServiceGrpc.StakeholdersServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        blockingStub = StakeholdersServiceGrpc.newBlockingStub(channel);

        System.out.println("✅ gRPC Client povezan na Stakeholders Service: " + host + ":" + port);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            System.out.println("🔌 gRPC Client zatvoren");
        }
    }

    public boolean checkUserExists(String username) {
        try {
            System.out.println("🔄 Gateway gRPC: Proveravam da li korisnik '" + username + "' postoji...");

            CheckUserExistsRequest request = CheckUserExistsRequest.newBuilder()
                    .setUsername(username)
                    .build();

            CheckUserExistsResponse response = blockingStub.checkUserExists(request);

            boolean exists = response.getExists();
            System.out.println("✅ Gateway gRPC: Korisnik '" + username + "' " + (exists ? "POSTOJI" : "NE POSTOJI"));

            return exists;

        } catch (Exception e) {
            System.err.println("❌ Gateway gRPC greška: " + e.getMessage());
            return false;
        }
    }

    public boolean isUserBlocked(String username) {
        try {
            System.out.println("🔄 Gateway gRPC: Proveravam da li je korisnik '" + username + "' blokiran...");

            IsUserBlockedRequest request = IsUserBlockedRequest.newBuilder()
                    .setUsername(username)
                    .build();

            IsUserBlockedResponse response = blockingStub.isUserBlocked(request);

            boolean blocked = response.getBlocked();
            System.out.println("✅ Gateway gRPC: Korisnik '" + username + "' je " + (blocked ? "BLOKIRAN" : "AKTIVAN"));

            return blocked;

        } catch (Exception e) {
            System.err.println("❌ Gateway gRPC greška: " + e.getMessage());
            return false;
        }
    }

    public String getUserRole(String username) {
        try {
            System.out.println("🔄 Gateway gRPC: Dohvatam ulogu korisnika '" + username + "'...");

            GetUserRoleRequest request = GetUserRoleRequest.newBuilder()
                    .setUsername(username)
                    .build();

            GetUserRoleResponse response = blockingStub.getUserRole(request);

            String role = response.getRole();
            System.out.println("✅ Gateway gRPC: Uloga korisnika '" + username + "' je " + role);

            return role;

        } catch (Exception e) {
            System.err.println("❌ Gateway gRPC greška: " + e.getMessage());
            return null;
        }
    }
}

