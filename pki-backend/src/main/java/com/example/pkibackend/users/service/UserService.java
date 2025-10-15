package com.example.pkibackend.users.service;

import com.example.pkibackend.certificates.dtos.IssuerDTO;
import com.example.pkibackend.certificates.dtos.UserDTO;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.certificates.repository.CertificateRepository;
import com.example.pkibackend.certificates.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final RestTemplate rest = new RestTemplate();

    @Autowired
    private CertificateRepository certificateRepository;

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
        issuerDto.setOrganization((String) user.get("organization"));
        issuerDto.setDepartment((String) user.get("department"));

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

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getLoggedUser() {
        JwtAuthenticationToken token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return this.findUserByEmail(token.getTokenAttributes().get("email").toString());
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User data) {
        return this.userRepository.save(data);
    }



    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDTO(user.getId(),
                        user.getKeycloakId(), // Vraćamo i keycloakId
                        String.format("%s %s (%s)", user.getFirstname(), user.getLastname(), user.getEmail()), user.getOrganization()) )
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignCertificateToUser(Integer userId, String serialNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Certificate certificate = certificateRepository.findById(serialNumber)
                .orElseThrow(() -> new RuntimeException("Certificate not found with serial number: " + serialNumber));

        user.getCertificates().add(certificate);
        userRepository.save(user);
    }
}
