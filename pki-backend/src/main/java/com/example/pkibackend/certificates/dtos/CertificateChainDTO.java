package com.example.pkibackend.certificates.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateChainDTO {
    private CertificateInfoDTO rootCertificate;
    private List<CertificateInfoDTO> issuedCertificates;
}