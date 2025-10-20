package com.example.pkibackend.util;

import com.example.pkibackend.certificates.model.Organization;
import com.example.pkibackend.certificates.service.OrganizationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Component
public class Encryption {

    @Value("${pki.encryption.master-key}")
    private static String masterKeyBase64;
    private byte[] MASTER_KEY;
    private final int GCM_TAG_LENGTH = 16;
    private final int GCM_NONCE_LENGTH = 12;

    @PostConstruct
    public void init() {
        String keyBase64 = System.getenv("PKI_ENCRYPTION_MASTER_KEY");
        if (keyBase64 == null || keyBase64.isEmpty()) {
            keyBase64 = masterKeyBase64;
        }
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new IllegalStateException("Master key not found in environment or application.properties");
        }
        try {
            MASTER_KEY = Base64.getDecoder().decode(keyBase64);
            if (MASTER_KEY.length != 32) {
                throw new IllegalStateException("Master key must be 256 bits (32 bytes), got " + MASTER_KEY.length + " bytes");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Base64 encoding for master key", e);
        }
    }


    public String encryptOrganizationKey(String plaintextKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(plaintextKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding for plaintext key", e);
        }
        SecretKeySpec masterKeySpec = new SecretKeySpec(MASTER_KEY, "AES");

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, masterKeySpec, gcmSpec);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        byte[] encryptedBytes = null;
        try {
            encryptedBytes = cipher.doFinal(keyBytes);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }

        byte[] combined = new byte[nonce.length + encryptedBytes.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(encryptedBytes, 0, combined, nonce.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decryptOrganizationKey(String encryptedKey) {
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(encryptedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding for encrypted key", e);
        }

        if (combined.length < GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("Encrypted key data is too short");
        }
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_NONCE_LENGTH];
        System.arraycopy(combined, 0, nonce, 0, GCM_NONCE_LENGTH);
        System.arraycopy(combined, GCM_NONCE_LENGTH, ciphertext, 0, combined.length - GCM_NONCE_LENGTH);

        SecretKeySpec masterKeySpec = new SecretKeySpec(MASTER_KEY, "AES");
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);

        try {
            cipher.init(Cipher.DECRYPT_MODE, masterKeySpec, gcmSpec);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        byte[] decryptedBytes;
        try {
            decryptedBytes = cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed: invalid key or corrupted data", e);
        }

        return Base64.getEncoder().encodeToString(decryptedBytes);
    }

    public String generateOrganizationKey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.init(256, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    public String encryptPrivateKey(PrivateKey privateKey, String organizationKey) {
        if (privateKey == null || organizationKey == null) {
            throw new IllegalArgumentException("Private key and organization key must not be null");
        }

        byte[] privateKeyBytes = privateKey.getEncoded();

        byte[] orgKeyBytes;
        try {
            orgKeyBytes = Base64.getDecoder().decode(organizationKey);
            if (orgKeyBytes.length != 32) {
                throw new IllegalArgumentException("Organization key must be 256 bits (32 bytes)");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding for organization key", e);
        }

        SecretKeySpec orgKeySpec = new SecretKeySpec(orgKeyBytes, "AES");
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Error setting up the cipher", e);
        }

        byte[] nonce = new byte[GCM_NONCE_LENGTH]; // 12 bytes for AES-GCM
        new SecureRandom().nextBytes(nonce); // Generate random nonce
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, orgKeySpec, gcmSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Error initializing the cipher", e);
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(privateKeyBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Error encrypting private key", e);
        }

        // Combine nonce + encrypted data (ciphertext)
        byte[] combined = new byte[nonce.length + encryptedBytes.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(encryptedBytes, 0, combined, nonce.length, encryptedBytes.length);

        // Base64 encode and return
        return Base64.getEncoder().encodeToString(combined);
    }

    public PrivateKey decryptPrivateKey(String encryptedPrivateKey, String organizationKey) {
        if (encryptedPrivateKey == null || organizationKey == null) {
            throw new IllegalArgumentException("Encrypted private key and organization key must not be null");
        }

        byte[] combined;
        try {
            // Decode Base64 string to byte array
            combined = Base64.getDecoder().decode(encryptedPrivateKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding for encrypted private key", e);
        }

        // Ensure combined data is of correct length (nonce + ciphertext + tag)
        if (combined.length <= GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("Encrypted private key data is too short");
        }

        // The nonce is the first part (12 bytes for AES-GCM)
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        System.arraycopy(combined, 0, nonce, 0, GCM_NONCE_LENGTH);

        // The ciphertext is the remaining data (encrypted private key)
        byte[] ciphertext = new byte[combined.length - GCM_NONCE_LENGTH];
        System.arraycopy(combined, GCM_NONCE_LENGTH, ciphertext, 0, ciphertext.length);

        byte[] orgKeyBytes;
        try {
            // Decode the organization key from Base64
            orgKeyBytes = Base64.getDecoder().decode(organizationKey);
            if (orgKeyBytes.length != 32) {
                throw new IllegalArgumentException("Organization key must be 256 bits (32 bytes)");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding for organization key", e);
        }

        SecretKeySpec orgKeySpec = new SecretKeySpec(orgKeyBytes, "AES");
        Cipher cipher;
        try {
            // Create cipher instance with AES-GCM/NoPadding
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Cipher initialization failed", e);
        }

        // Initialize the GCMParameterSpec with the nonce (12 bytes) and authentication tag length (16 bytes)
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);

        try {
            cipher.init(Cipher.DECRYPT_MODE, orgKeySpec, gcmSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Error initializing cipher for decryption", e);
        }

        byte[] decryptedBytes;
        try {
            // Decrypt the ciphertext
            decryptedBytes = cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed: invalid key or corrupted data", e);
        }

        // Reconstruct the private key from decrypted bytes
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstruct private key from decrypted data", e);
        }
    }
}
