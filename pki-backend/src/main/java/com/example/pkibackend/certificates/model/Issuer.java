package com.example.pkibackend.certificates.model;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.example.pkibackend.certificates.service.OrganizationService;
import com.example.pkibackend.util.Encryption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "issuers")
public class Issuer {

    @Id
    private String userUUID;

    @Transient
    private PrivateKey privateKey;

    @Column(nullable = false, length = 1664)
    private String encPrivateKey;

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

    public PrivateKey getPrivateKey(Encryption encryption, OrganizationService organizationService) {
        LdapName ldapName = null;
        try {
            ldapName = new LdapName(this.x500NameString);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }

        for (Rdn rdn : ldapName.getRdns()) {
            if ("O".equals(rdn.getType())) {
                String orgName = (String) rdn.getValue();
                this.privateKey = encryption.decryptPrivateKey(this.encPrivateKey, organizationService.createIfNotExist(orgName).getOrganizationKey(encryption));
            }
        }
        return privateKey;
    }
}
