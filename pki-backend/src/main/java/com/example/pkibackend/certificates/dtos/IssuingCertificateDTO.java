package com.example.pkibackend.certificates.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IssuingCertificateDTO {
    private String serialNumber;
    private String subject;
}