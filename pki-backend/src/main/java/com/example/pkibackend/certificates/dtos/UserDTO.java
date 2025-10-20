package com.example.pkibackend.certificates.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String displayName;
    private String keycloakId;
    private String organization;
}