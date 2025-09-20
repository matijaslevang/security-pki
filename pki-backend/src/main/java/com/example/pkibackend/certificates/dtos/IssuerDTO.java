package com.example.pkibackend.certificates.dtos;

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
}
