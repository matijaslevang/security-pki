package com.example.pkibackend.certificates.dtos;

import com.example.pkibackend.certificates.model.enums.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateInfoDTO {
    private String serialNumber;
    private String subject;
    private String issuer;
    private Date validFrom;
    private Date validTo;
    private Boolean isCa;
    private CertificateStatus status;
}