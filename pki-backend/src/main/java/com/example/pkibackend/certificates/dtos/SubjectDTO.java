package com.example.pkibackend.certificates.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubjectDTO {
    private String commonName;
    private String surname;
    private String givenName;
    private String organization;
    private String department;
    private String email;
    private String country = "RS";

    public SubjectDTO(IssuerDTO issuerDTO) {
        this.surname = issuerDTO.getSurname();
        this.givenName = issuerDTO.getGivenName();
        this.organization = issuerDTO.getOrganization();
        this.department = issuerDTO.getDepartment();
        this.email = issuerDTO.getEmail();
        this.country = issuerDTO.getCountry();
    }
}
