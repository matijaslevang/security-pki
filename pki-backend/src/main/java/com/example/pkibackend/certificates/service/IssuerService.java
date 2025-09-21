package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.IssuerDTO;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.repository.IssuerRepository;
import com.example.pkibackend.util.DTOToX500Name;
import com.example.pkibackend.util.KeyPairGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

@Service
public class IssuerService {
    @Autowired
    private IssuerRepository issuerRepository;

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
}
