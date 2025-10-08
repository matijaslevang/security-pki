package com.example.pkibackend.certificates.dtos;

import com.example.pkibackend.certificates.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssuerDTO {
    private String uuid;
    private String surname;
    private String givenName;
    private String organization;
    private String department;
    private String email;
    private String country = "RS";

    public IssuerDTO(User user) {
        this.uuid = user.getKeycloakId();
        this.surname = user.getLastname();
        this.givenName = user.getFirstname();
        this.organization = user.getOrganization();
        this.department = user.getDepartment();
        this.email = user.getEmail();
    }
}
