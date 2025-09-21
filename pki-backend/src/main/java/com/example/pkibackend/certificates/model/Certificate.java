package com.example.pkibackend.certificates.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "certificates")
public class Certificate {
    @Id
    private BigInteger serial;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String issuerId;

    @Column(nullable = false)
    private X509Certificate x509Certificate;
}
