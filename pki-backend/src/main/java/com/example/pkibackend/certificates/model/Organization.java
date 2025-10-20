package com.example.pkibackend.certificates.model;

import com.example.pkibackend.util.Encryption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Transient
    private String organizationKey;

    @Column(nullable = false, length = 512)
    private String encOrganizationKey;

    public Organization(String name, Encryption encryption) {
        this.name = name;
        this.organizationKey = encryption.generateOrganizationKey();
        this.encOrganizationKey = encryption.encryptOrganizationKey(this.organizationKey);
    }

    public String getOrganizationKey(Encryption encryption) {
        return encryption.decryptOrganizationKey(this.encOrganizationKey);
    }
}
