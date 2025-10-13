package com.example.pkibackend.util;

import com.example.pkibackend.certificates.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

@Converter
@Component
public class PrivateKeyConverter implements AttributeConverter<PrivateKey, byte[]> {

    private static EncryptionService encryptionService;

    // Statički setter da Spring može da inject-uje servis
    @Autowired
    public void setEncryptionService(EncryptionService service) {
        PrivateKeyConverter.encryptionService = service;
    }

    @Override
    public byte[] convertToDatabaseColumn(PrivateKey privateKey) {
        if (privateKey == null) {
            return null;
        }
        try {
            // 1. Pretvori PrivateKey u niz bajtova
            byte[] privateKeyBytes = privateKey.getEncoded();
            // 2. Enkriptuj niz bajtova
            return encryptionService.encrypt(privateKeyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert private key to byte[]", e);
        }
    }

    @Override
    public PrivateKey convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // 1. Dekriptuj niz bajtova iz baze
            byte[] decryptedBytes = encryptionService.decrypt(dbData);
            // 2. Pretvori dekriptovane bajtove nazad u PrivateKey objekat
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Ili "EC", zavisno od toga šta generišeš
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decryptedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Could not convert byte[] to private key", e);
        }
    }
}