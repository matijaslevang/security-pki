package com.example.pkibackend.certificates.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.pkibackend.certificates.model.enums.RevocationReason;
import com.example.pkibackend.certificates.model.enums.CertificateStatus;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "certificates")
public class Certificate {
    @Id
    private String serial;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String issuerId;

    @Column(nullable = false)
    private X509Certificate x509Certificate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status = CertificateStatus.VALID;

    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    private RevocationReason revocationReason;

    @ManyToMany(mappedBy = "certificates", fetch = FetchType.LAZY)
    @JsonIgnore // Sprečava beskonačnu rekurziju prilikom serijalizacije
    private Set<User> users;
}
