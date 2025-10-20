package com.example.pkibackend.certificates.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CertificateChainDisplayDTO {
    private List<CertificateRowDTO> chainRows;
}