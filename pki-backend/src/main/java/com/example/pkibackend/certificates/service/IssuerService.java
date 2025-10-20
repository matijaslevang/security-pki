package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.IssuerDTO;
import com.example.pkibackend.certificates.dtos.SubjectDTO;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Organization;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.repository.IssuerRepository;
import com.example.pkibackend.util.DTOToX500Name;
import com.example.pkibackend.util.Encryption;
import com.example.pkibackend.util.KeyPairGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

@Service
public class IssuerService {
    @Autowired
    private IssuerRepository issuerRepository;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private Encryption encryption;

    public Issuer getIssuer(String uuid) {
        return issuerRepository.findById(uuid).orElse(null);
    }

    public Issuer createIfNotExist(IssuerDTO issuerDTO) {
        Issuer issuer = getIssuer(issuerDTO.getUuid());
        if (issuer != null) {
            return issuer;
        }
        issuer = new Issuer();
        issuer.setUserUUID(issuerDTO.getUuid());
        issuer.setX500Name(DTOToX500Name.IssuerDTOToX500Name(issuerDTO));
        KeyPair keyPair = KeyPairGenerator.generateKeyPair();
        issuer.setPublicKey(keyPair.getPublic());
        issuer.setPrivateKey(keyPair.getPrivate());
        return issuerRepository.save(issuer);
    }

    public Issuer createIfNotExistForSelfSigned(SubjectDTO subjectDTO) {
        Issuer issuer = new Issuer();
        String userUUID;
        do {
            userUUID = UUID.randomUUID().toString();
        } while (getIssuer(userUUID) != null);
        issuer.setUserUUID(userUUID);
        issuer.setX500Name(DTOToX500Name.SubjectDTOToX500Name(subjectDTO));
        KeyPair keyPair = KeyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        issuer.setPublicKey(publicKey);
        PrivateKey privateKey = keyPair.getPrivate();
        issuer.setPrivateKey(privateKey);

        LdapName ldapName = null;
        try {
            ldapName = new LdapName(issuer.getX500Name().toString());
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }

        for (Rdn rdn : ldapName.getRdns()) {
            if ("O".equals(rdn.getType())) {
                String orgName = (String) rdn.getValue();
                Organization organization = organizationService.createIfNotExist(orgName);
                issuer.setEncPrivateKey(encryption.encryptPrivateKey(privateKey, organization.getOrganizationKey(encryption)));
            }
        }

        subjectService.createForSelfSigned(subjectDTO, publicKey);

        return issuerRepository.save(issuer);
    }
}
