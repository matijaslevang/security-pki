package com.example.pkibackend.certificates.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Set;

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

    @ManyToMany(mappedBy = "certificates", fetch = FetchType.LAZY)
    @JsonIgnore // Sprečava beskonačnu rekurziju prilikom serijalizacije
    private Set<User> users;
}
