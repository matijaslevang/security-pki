package com.example.pkibackend.users.service;

import com.example.pkibackend.certificates.dtos.IssuerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${env_keycloak_url}")
    private String keycloakUrl;

    @Value("${env_realm_name}")
    private String realm;

    @Value("${env_client_id}")
    private String clientId;

    @Value("${env_client_secret}")
    private String clientSecret;

    public IssuerDTO getIssuer(Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        Map tokenResponse = rest.postForObject(
                keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                params,
                Map.class
        );

        String accessToken = (String) tokenResponse.get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = rest.exchange(
                keycloakUrl + "/admin/realms/" + realm + "/users/" + userId,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> user = response.getBody();

        IssuerDTO issuerDto = new IssuerDTO();

        issuerDto.setUuid((String) user.get("id"));
        issuerDto.setGivenName((String) user.get("firstName"));
        issuerDto.setSurname((String) user.get("lastName"));
        issuerDto.setEmail((String) user.get("email"));

        Map<String, List<String>> attributes = (Map<String, List<String>>) user.get("attributes");
        if (attributes != null) {
            issuerDto.setOrganization(attributes.getOrDefault("organization", List.of())
                    .stream().findFirst().orElse(null));
            issuerDto.setDepartment(attributes.getOrDefault("department", List.of())
                    .stream().findFirst().orElse(null));
        }

        // country defaults to "RS" as per your class
        return issuerDto;
    }
}
