package com.example.pkibackend.security;

import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.users.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class JwtUserFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            JwtAuthenticationToken token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = String.valueOf(token.getTokenAttributes().get("email"));
            User user = this.userService.findUserByEmail(email);

            if (user == null) {
                String keycloakId = String.valueOf(token.getTokenAttributes().get("sub"));
                String firstname = String.valueOf(token.getTokenAttributes().get("given_name"));
                String lastname = String.valueOf(token.getTokenAttributes().get("family_name"));

                // ISPRAVLJENA LOGIKA ZA ČITANJE ATRIBUTA
                Map<String, Object> attributes = (Map<String, Object>) token.getTokenAttributes().get("attributes");
                String organization = null;
                String department = null;

                if (attributes != null) {
                    if (attributes.get("organization") instanceof List && !((List<?>) attributes.get("organization")).isEmpty()) {
                        organization = ((List<String>) attributes.get("organization")).get(0);
                    }
                    if (attributes.get("department") instanceof List && !((List<?>) attributes.get("department")).isEmpty()) {
                        department = ((List<String>) attributes.get("department")).get(0);
                    }
                }

                this.userService.save(new User(null, keycloakId, email, firstname, lastname, organization, department, new HashSet<>()));
            }
        } catch (Exception e) {
            // Logujte grešku umesto da je bacate, da ne biste prekinuli filter chain
            logger.error("Error processing JWT user filter", e);
        }

        filterChain.doFilter(request, response);
    }
}
