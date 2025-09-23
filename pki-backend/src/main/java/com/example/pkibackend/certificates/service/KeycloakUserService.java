package com.example.pkibackend.certificates.service;


import com.example.pkibackend.certificates.dtos.KcUserDto;
import com.example.pkibackend.certificates.dtos.UserDTO;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KeycloakUserService {

    @Value("${env_keycloak_url}")
    private String kcUrl;          // npr. http://localhost:7778
    @Value("${env_realm_name}")
    private String realm;          // npr. security-pki
    @Value("${env_client_id}")
    private String adminClientId;  // npr. pki-admin
    @Value("${env_client_secret}")
    private String adminSecret;    // secret za pki-admin

    private final WebClient web = WebClient.builder().build();

    /** Svi korisnici koji imaju BAREM jednu od navedenih REALM rola. */
    public List<KcUserDto> usersWithAnyRole(List<String> roles) {
        String token = adminToken();
        Map<String, KcUserDto> unique = new HashMap<>();
        for (String role : roles) {
            for (KcUserDto u : usersWithRealmRole(role, token)) {
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                System.out.println(u.username());
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                unique.putIfAbsent(u.id(), u);
            }
        }
        return new ArrayList<>(unique.values());
    }

    // ===== internals =====

    private List<KcUserDto> usersWithRealmRole(String role, String token) {
        String uri = kcUrl + "/admin/realms/" + realm + "/roles/" + url(role) + "/users?first=0&max=200";

        List<Map<String, Object>> raw = web.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();

        if (raw == null) return List.of();

        return raw.stream()
                .map(m -> new KcUserDto(
                        s(m.get("id")),
                        s(m.get("username")),
                        s(m.get("email")),
                        s(m.get("firstName")),
                        s(m.get("lastName"))
                ))
                .collect(Collectors.toList());
    }

    private String adminToken() {
        String tokenUri = kcUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        String body = "grant_type=client_credentials"
                + "&client_id=" + url(adminClientId)
                + "&client_secret=" + url(adminSecret);

        TokenResponse tr = web.post()
                .uri(tokenUri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (tr == null || tr.access_token == null) {
            throw new IllegalStateException("Cannot obtain Keycloak admin token");
        }
        return tr.access_token;
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String url(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }

    // minimal token DTO
    static final class TokenResponse {
        public String access_token;
        public String token_type;
        public long   expires_in;
        public String scope;
    }
}