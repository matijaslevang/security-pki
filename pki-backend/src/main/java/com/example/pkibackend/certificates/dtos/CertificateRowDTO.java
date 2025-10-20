package com.example.pkibackend.certificates.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CertificateRowDTO {
    private CertificateInfoDTO certificate;
    private int depth; // 0 za Root, 1 za Intermediate, 2 za End-Entity, itd.
}