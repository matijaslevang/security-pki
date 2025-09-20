package com.example.pkibackend.certificates.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.asn1.x500.X500Name;

import java.security.PublicKey;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private PublicKey publicKey;

    @Column(nullable = false)
    private String x500NameString;

    public void setX500Name(X500Name x500Name) {
        this.x500NameString = x500Name.toString();
    }

    public X500Name getX500Name() {
        return new X500Name(x500NameString);
    }
}
