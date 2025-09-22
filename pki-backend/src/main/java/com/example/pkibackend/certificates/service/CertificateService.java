package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.CreateCertificateDTO;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.repository.CertificateRepository;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private IssuerService issuerService;

    @Autowired
    private CertificateRepository certificateRepository;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public Certificate createCertificate(CreateCertificateDTO createCertificateDTO) {
        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        builder = builder.setProvider("BC");
        Issuer issuer = issuerService.getIssuer(createCertificateDTO.getIssuerUuid());

        if (issuer == null) {
            return null;
        }

        ContentSigner contentSigner;
        JcaX509ExtensionUtils extUtils;
        try {
            contentSigner = builder.build(issuer.getPrivateKey());
            extUtils = new JcaX509ExtensionUtils();
        } catch (OperatorCreationException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        BigInteger serial;
        do {
            UUID uuid = UUID.randomUUID();
            serial = new BigInteger(uuid.toString().replace("-", ""), 16);
            if (serial.signum() < 0) {
                serial = serial.negate(); // ensure positive serial number
            }
        } while (getCertificate(serial) != null);

        Subject subject = subjectService.createIfNotExist(createCertificateDTO.getSubjectDTO());

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                issuer.getX500Name(),
                serial,
                createCertificateDTO.getStartDate(),
                createCertificateDTO.getEndDate(),
                subject.getX500Name(),
                subject.getPublicKey()
        );

        try {
            if (createCertificateDTO.isSelfSigned()) {
                certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
                certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.cRLSign));
            } else if (createCertificateDTO.isIntermediate()) {
                // TODO: code for intermediate certificate
            } else {
                certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
                certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            }

            if (createCertificateDTO.isSkiaki()) {
                certGen.addExtension(Extension.subjectKeyIdentifier, false,
                        extUtils.createSubjectKeyIdentifier(subject.getPublicKey()));
                certGen.addExtension(Extension.authorityKeyIdentifier, false,
                        extUtils.createAuthorityKeyIdentifier(issuer.getPublicKey()));
            }

            if (!(createCertificateDTO.getSanString() == null || createCertificateDTO.getSanString().isEmpty())) {
                List<String> sanList = Arrays.stream(createCertificateDTO.getSanString().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                GeneralName[] sanNameList = sanList.stream().map(name -> new GeneralName(GeneralName.dNSName, name)).toArray(GeneralName[]::new);
                GeneralNames sanNames = new GeneralNames(sanNameList);
                certGen.addExtension(Extension.subjectAlternativeName, false, sanNames);
            }

        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }

        X509CertificateHolder certHolder = certGen.build(contentSigner);

        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        certConverter = certConverter.setProvider("BC");

        X509Certificate certificate;
        try {
            certificate = certConverter.getCertificate(certHolder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        Certificate certificateWrapper = new Certificate(serial, subject.getId(), issuer.getUserUUID(), certificate);

        // TODO: further encrypt certificate

        return certificateRepository.save(certificateWrapper);
    }

    public Certificate findCertificateByIssuerAndSubject(Long subjectId, String issuerId) {
        return certificateRepository.findByIssuerIdAndSubjectId(issuerId, subjectId);
    }

    public Certificate findBySubjectId(Long subjectId) {
        return certificateRepository.findBySubjectId(subjectId);
    }

    public Certificate getCertificate(BigInteger serial) {
        return certificateRepository.findById(serial).orElse(null);
    }
}
