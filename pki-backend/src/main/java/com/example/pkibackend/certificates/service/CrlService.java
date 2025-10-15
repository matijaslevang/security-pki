package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.enums.CertificateStatus;
import com.example.pkibackend.certificates.repository.CertificateRepository;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Time;
@Service
public class CrlService {
    private final CertificateRepository certificateRepository;
    private final IssuerService issuerService;

    // npr. application.properties: pki.crl.cdp-url=http://localhost:7777/api/crl/latest
    @Value("${pki.crl.cdp-url:http://localhost:7777/api/crl/latest}")
    private String publicCrlUrl;

    private volatile byte[] latestCrlDer = null;
    private volatile Instant lastBuilt = null;

    public CrlService(CertificateRepository certificateRepository, IssuerService issuerService) {
        this.certificateRepository = certificateRepository;
        this.issuerService = issuerService;
    }

    public String getPublicCrlUrl() {
        return publicCrlUrl;
    }

    /** Regeneriši CRL iz svih REVOKED sertifikata. */
    public synchronized void regenerateCrl() {
        try {
            // 1) Izaberi CRL issuer-a: uzmi neki aktivni CA koji je self-signed (root) ako postoji,
            //    u suprotnom bilo koji VAŽEĆI CA (basicConstraints >= 0)
            Certificate crlIssuerRecord = pickCrlIssuer();
            if (crlIssuerRecord == null) {
                // nema CA u sistemu → nema smisla graditi CRL
                latestCrlDer = emptyCrl(); // ili ostavi null
                lastBuilt = Instant.now();
                return;
            }
            X509Certificate crlIssuerX = crlIssuerRecord.getX509Certificate();
            Issuer crlIssuer = issuerService.getIssuer(crlIssuerRecord.getIssuerId());
            PrivateKey crlIssuerKey = crlIssuer.getPrivateKey();

            Date now = new Date();
            X500Name issuerName = new X500Name(crlIssuerX.getSubjectX500Principal().getName());
            X509v2CRLBuilder builder = new X509v2CRLBuilder(issuerName, now);

            // nextUpdate za ~24h
            builder.setNextUpdate(new Time(Date.from(Instant.now().plusSeconds(24 * 3600))));

            // 2) Dodaj sve REVOKED zapise
            List<Certificate> revoked = certificateRepository.findAll().stream()
                    .filter(c -> c.getStatus() == CertificateStatus.REVOKED)
                    .toList();

            for (Certificate c : revoked) {
                BigInteger serial = c.getX509Certificate().getSerialNumber();
                Date revTime = Date.from(c.getRevokedAt());
                int reasonCode = c.getRevocationReason() != null
                        ? c.getRevocationReason().code // ili .code ako je field
                        : CRLReason.unspecified;
                builder.addCRLEntry(serial, revTime, reasonCode);
            }

            // 3) Potpiši CRL
            String sigAlg = pickSigAlg(crlIssuerKey);
            ContentSigner signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(crlIssuerKey);
            X509CRL crl = new JcaX509CRLConverter().setProvider("BC").getCRL(builder.build(signer));
            latestCrlDer = crl.getEncoded();
            lastBuilt = Instant.now();

            System.out.println("[CRL] issuer=" + crlIssuerX.getSubjectX500Principal());
            System.out.println("[CRL] revoked count=" + revoked.size());

        } catch (Exception e) {
            throw new RuntimeException("CRL build failed", e);
        }
    }

    /** Vrati poslednji CRL u DER formatu. Ako ne postoji, regeneriši. */
    public byte[] getLatestCrlDer() {
        if (latestCrlDer == null) regenerateCrl();
        return latestCrlDer;
    }

    /** Pomoćno: izaberi ko potpisuje CRL. */
    private Certificate pickCrlIssuer() {
        List<Certificate> all = certificateRepository.findAll();
        // prvo probaj self-signed CA
        for (Certificate c : all) {
            X509Certificate x = c.getX509Certificate();
            if (x == null) continue;
            if (x.getBasicConstraints() >= 0
                    && x.getSubjectX500Principal().equals(x.getIssuerX500Principal())
                    && c.getStatus() == CertificateStatus.VALID) {
                return c;
            }
        }
        // ako nema root-a, uzmi bilo koji VALID CA
        for (Certificate c : all) {
            X509Certificate x = c.getX509Certificate();
            if (x == null) continue;
            if (x.getBasicConstraints() >= 0 && c.getStatus() == CertificateStatus.VALID) return c;
        }
        return null;
    }

    private String pickSigAlg(PrivateKey issuerKey) {
        String alg = issuerKey.getAlgorithm();
        return "EC".equalsIgnoreCase(alg) ? "SHA256withECDSA" : "SHA256withRSA";
    }

    private byte[] emptyCrl() {
        return new byte[0];
    }
}
