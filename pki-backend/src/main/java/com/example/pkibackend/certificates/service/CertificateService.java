package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.*;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.certificates.model.enums.CertificateStatus;
import com.example.pkibackend.certificates.model.enums.RevocationReason;
import com.example.pkibackend.certificates.repository.CertificateRepository;
import com.example.pkibackend.certificates.repository.UserRepository;
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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
@Service
public class CertificateService {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private IssuerService issuerService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CrlService crlService;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

//    public Certificate createCertificate(CreateCertificateDTO dto) {
//        if (dto.getIssuerSerialNumber() == null || dto.getIssuerSerialNumber().isEmpty()) {
//            throw new IllegalArgumentException("Issuer serial number must be provided for this operation.");
//        }
//
//        Certificate signingCertificateRecord = certificateRepository
//                .findById(dto.getIssuerSerialNumber())
//                .orElseThrow(() -> new RuntimeException("Issuing certificate with serial number " + dto.getIssuerSerialNumber() + " not found."));
//
//        X509Certificate signingCertX509 = signingCertificateRecord.getX509Certificate();
//
//        if (signingCertX509.getBasicConstraints() < 0) {
//            throw new IllegalArgumentException("The selected certificate cannot be used to issue other certificates (it is not a CA).");
//        }
//
//        try {
//            signingCertX509.checkValidity();
//        } catch (CertificateException e) {
//            throw new RuntimeException("The selected signing certificate is not valid: " + e.getMessage());
//        }
//
//        boolean hasKeyCertSign = signingCertX509.getKeyUsage() != null
//                && signingCertX509.getKeyUsage().length > 5
//                && signingCertX509.getKeyUsage()[5];
//        if (!hasKeyCertSign) {
//            throw new IllegalArgumentException("The issuing certificate is missing the KeyCertSign permission.");
//        }
//
//        if (signingCertificateRecord.getStatus() == CertificateStatus.REVOKED) {
//            throw new IllegalArgumentException("The issuing certificate has been revoked and cannot issue new certificates.");
//        }
//
//        if (dto.getEndDate().after(signingCertX509.getNotAfter())) {
//            throw new IllegalArgumentException("End date of the issued certificate exceeds the issuer's validity period.");
//        }
//
//        String issuerUuidForSigningKey = signingCertificateRecord.getIssuerId();
//        Issuer issuerForSigning = issuerService.getIssuer(issuerUuidForSigningKey);
//
//        if (issuerForSigning == null) {
//            throw new RuntimeException("Could not find the owner (issuer) of the signing certificate.");
//        }
//
//        return this.createCertificate(dto, issuerForSigning);
//    }
    public Certificate createCertificate(CreateCertificateDTO dto) {
        // 1) Obavezni parametri
        if (dto.getIssuerSerialNumber() == null || dto.getIssuerSerialNumber().isEmpty())
            throw new IllegalArgumentException("Issuer serial number is required.");
        if (dto.getStartDate() == null || dto.getEndDate() == null)
            throw new IllegalArgumentException("Start/end date are required.");
        if (!dto.getEndDate().after(dto.getStartDate()))
            throw new IllegalArgumentException("End date must be after start date.");

        // 2) Nađi izdavaoca po serijskom broju
        Certificate issuerRecord = certificateRepository
                .findById(dto.getIssuerSerialNumber())
                .orElseThrow(() -> new RuntimeException("Issuing certificate not found."));

        X509Certificate issuerX = issuerRecord.getX509Certificate();

        // 3) Validacije izdavaoca
        if (issuerX == null) throw new IllegalStateException("Issuer X509 is missing.");
        if (issuerRecord.getStatus() == CertificateStatus.REVOKED)
            throw new IllegalArgumentException("Issuing certificate is revoked.");
        if (!isCa(issuerX))
            throw new IllegalArgumentException("Issuing certificate is not a CA.");
        if (!hasKeyCertSign(issuerX))
            throw new IllegalArgumentException("Issuing certificate lacks KeyCertSign.");
        try {
            issuerX.checkValidity(); // nije istekao i još ne važi u budućnosti
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Issuing certificate not valid now.");
        }

        // 4) Ograniči važenje izdatog sertifikata u okviru izdavaoca
        if (dto.getEndDate().after(issuerX.getNotAfter()))
            throw new IllegalArgumentException("End date exceeds issuer validity.");
        // opcionalno i donja granica:
        // if (dto.getStartDate().before(issuerX.getNotBefore()))
        //     throw new IllegalArgumentException("Start date is before issuer validity.");

        // 5) Učitaj privatni ključ izdavaoca
        Issuer issuer = issuerService.getIssuer(issuerRecord.getIssuerId());
        if (issuer == null) throw new RuntimeException("Issuer owner not found.");

        // 6) Delegiraj stvarnu izradu (ekstenzije, SKI/AKI, SAN, potpis)
        return createCertificate(dto, issuer);
    }

    // Pomagači (u istom servisu, private):
    private boolean isCa(X509Certificate x) { return x.getBasicConstraints() != -1; }

    private boolean hasKeyCertSign(X509Certificate x) {
        boolean[] ku = x.getKeyUsage();
        return ku != null && ku.length > 5 && ku[5];
    }


    public Certificate createCertificate(CreateCertificateDTO createCertificateDTO, Issuer issuer) {
        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        builder = builder.setProvider("BC");

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
        } while (certificateRepository.existsById(serial.toString()));

        Subject subject = subjectService.createIfNotExist(createCertificateDTO.getSubjectDto());

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

                certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
                certGen.addExtension(Extension.keyUsage, true, new KeyUsage(
                        KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.cRLSign
                ));

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
//                String cdpUrl = "http://localhost:7777/api/crl/latest";
//                DistributionPointName dpName = new DistributionPointName(
//                        new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, cdpUrl)));
//                DistributionPoint dp = new DistributionPoint(dpName, null, null);
//                CRLDistPoint cdp = new CRLDistPoint(new DistributionPoint[] { dp });
//                certGen.addExtension(Extension.cRLDistributionPoints, false, cdp);
            }

        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }

        // CDP (CRL Distribution Points)
        String cdpUrl = crlService.getPublicCrlUrl(); // vidi korak 5.c ispod
        DistributionPointName dpName = new DistributionPointName(
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, cdpUrl)));
        DistributionPoint dp = new DistributionPoint(dpName, null, null);
        CRLDistPoint cdp = new CRLDistPoint(new DistributionPoint[] { dp });
        try {
            certGen.addExtension(Extension.cRLDistributionPoints, false, cdp);
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

        Certificate certificateWrapper = new Certificate();
        certificateWrapper.setSerial(serial.toString());
        certificateWrapper.setSubjectId(subject.getId());
        certificateWrapper.setIssuerId(issuer.getUserUUID());
        certificateWrapper.setX509Certificate(certificate);

        return certificateRepository.save(certificateWrapper);
    }

    public void revoke(String serial, int reasonCode, String requesterKcUuid, boolean isAdmin) {
        Certificate c = certificateRepository.findById(serial)
                .orElseThrow(() -> new RuntimeException("Certificate not found."));

        if (c.getStatus() == CertificateStatus.REVOKED)
            throw new IllegalStateException("Already revoked.");

        // Ko sme da povuče:
        // - admin: sve
        // - issuer-owner: ako ovaj korisnik ima issuerId == requester
        // - subject-owner: ako je ovaj korisnik vlasnik subjekta
        boolean allowed = isAdmin
                || requesterKcUuid.equals(c.getIssuerId())
                || isSubjectOwner(requesterKcUuid, c.getSubjectId());

        if (!allowed) throw new AccessDeniedException("Forbidden");

        c.setStatus(CertificateStatus.REVOKED);
        c.setRevokedAt(Instant.now());
        c.setRevocationReason(RevocationReason.fromCode(reasonCode));
        certificateRepository.save(c);
        crlService.regenerateCrl();
    }

    private boolean isSubjectOwner(String kcUuid, Long subjectId) {
        // Ako već imaš SubjectService → mapiraj subjectId -> user; ili koristi UserRepository
        // Minimalna verzija: korisnik mora imati ovaj cert u svom setu:
        return userRepository.findByKeycloakId(kcUuid)
                .map(u -> u.getCertificates().stream().anyMatch(cert -> cert.getSubjectId().equals(subjectId)))
                .orElse(false);
    }

    public Certificate findCertificateByIssuerAndSubject(Long subjectId, String issuerId) {
        return certificateRepository.findByIssuerIdAndSubjectId(issuerId, subjectId);
    }

    public List<Certificate> findBySubjectId(Long subjectId) {
        return certificateRepository.findBySubjectId(subjectId);
    }

    public Certificate getCertificate(String serial) {
        return certificateRepository.findById(serial).orElse(null);
    }


    public byte[] getCertificateAsKeyStore(String serialNumber, String password) {

        // 1. Pronađi sertifikat u bazi
        Certificate certificateRecord = certificateRepository.findById(serialNumber)
                .orElseThrow(() -> new RuntimeException("Certificate not found with serial number: " + serialNumber));

        if (certificateRecord.getStatus() == CertificateStatus.REVOKED) {
            throw new RuntimeException("Revoked certificates cannot be downloaded.");
        }

        // 2. Pronađi issuera da bismo dobili privatni ključ
        // Tvoj AttributeConverter će automatski dekriptovati ključ ovde!
        Issuer issuer = issuerService.getIssuer(certificateRecord.getIssuerId());

        PrivateKey privateKey = issuer.getPrivateKey();
        X509Certificate certificate = certificateRecord.getX509Certificate();

        try {
            // 3. Kreiraj novi, prazan PKCS12 KeyStore u memoriji
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // Inicijalizacija praznog keystore-a

            // 4. Kreiraj lanac sertifikata. Za sada, sadrži samo jedan sertifikat.
            // U složenijim scenarijima, ovde bi se dodavao i sertifikat issuera, itd.
            java.security.cert.Certificate[] certificateChain = { certificate };

            // 5. Ubaci privatni ključ i lanac sertifikata u KeyStore
            // Alias je "prijateljsko ime" za unos unutar keystore-a
            String alias = serialNumber.toString();
            keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), certificateChain);

            // 6. "Sačuvaj" KeyStore u memorijski stream (ByteArrayOutputStream)
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keyStore.store(bos, password.toCharArray());

            // 7. Vrati KeyStore kao niz bajtova
            return bos.toByteArray();

        } catch (Exception e) {
            // U praksi, koristi specifičnije izuzetke
            throw new RuntimeException("Error creating KeyStore", e);
        }
    }

    public List<CertificateInfoDTO> getCertificatesForUser(Long userId) {
        // Pronalazimo sve sertifikate gde je ulogovani korisnik bio izdavalac (issuer)
        List<Certificate> userCertificates = certificateRepository.findBySubjectId(userId);

        // Mapiramo svaki Certificate entitet u DTO
        return userCertificates.stream()
                .map(this::mapCertificateToDTO)
                .collect(Collectors.toList());
    }

    private CertificateInfoDTO mapCertificateToDTO(Certificate certificate) {
        X509Certificate x509Cert = certificate.getX509Certificate();
        return new CertificateInfoDTO(
                x509Cert.getSerialNumber().toString(),
                x509Cert.getSubjectX500Principal().getName(), // Dobijamo Subject
                x509Cert.getIssuerX500Principal().getName(),  // Dobijamo Issuer
                x509Cert.getNotBefore(), // Važi od
                x509Cert.getNotAfter() ,// Važi do
                x509Cert.getBasicConstraints() != -1,
                certificate.getStatus()
        );
    }

    public List<CertificateChainDTO> getCertificateChainsForCaUser(String caUserId) {
        User caUser = userRepository.findByKeycloakId(caUserId).orElse(null);

        if (caUser == null) {
            System.out.println("User with Keycloak ID " + caUserId + " not found in local DB.");
            return Collections.emptyList();
        }

        Set<Certificate> assignedCertificates = caUser.getCertificates();

        List<Certificate> userCaCerts = assignedCertificates.stream()
                .filter(cert -> {
                    X509Certificate x509 = cert.getX509Certificate();
                    if (x509 == null) return false;
                    // Ispravka: Pronalazimo SVE CA sertifikate dodeljene korisniku,
                    // bez obzira da li su self-signed (Root) ili ne (Intermediate).
                    return x509.getBasicConstraints() != -1;
                })
                .collect(Collectors.toList());

        List<Certificate> allCertificatesInSystem = certificateRepository.findAll();

        return userCaCerts.stream()
                .map(caCert -> {
                    CertificateInfoDTO caDto = mapCertificateToDTO(caCert);

                    List<CertificateInfoDTO> issuedCertificates = allCertificatesInSystem.stream()
                            .filter(cert -> {
                                if (cert.getX509Certificate() == null || cert.getX509Certificate().getIssuerX500Principal() == null) return false;
                                // Pronalazimo sve sertifikate koje je ovaj CA sertifikat izdao
                                return cert.getX509Certificate().getIssuerX500Principal().getName().equals(caDto.getSubject());
                            })
                            .map(this::mapCertificateToDTO)
                            .collect(Collectors.toList());

                    return new CertificateChainDTO(caDto, issuedCertificates);
                })
                .collect(Collectors.toList());
    }

    public List<CertificateChainDisplayDTO> getCertificateChainsForAdmin() {
        // 1. Dobavi sve sertifikate i mapiraj ih radi lakše pretrage
        List<Certificate> allCerts = certificateRepository.findAll();
        Map<String, List<Certificate>> certsByIssuer = allCerts.stream()
                .collect(Collectors.groupingBy(c -> c.getX509Certificate().getIssuerX500Principal().getName()));

        // 2. Pronađi sve Root (self-signed) sertifikate, oni su početak svakog lanca
        List<Certificate> rootCerts = allCerts.stream()
                .filter(c -> c.getX509Certificate().getIssuerX500Principal().equals(c.getX509Certificate().getSubjectX500Principal()))
                .collect(Collectors.toList());

        List<CertificateChainDisplayDTO> allChains = new ArrayList<>();

        // 3. Za svaki root, rekurzivno izgradi njegovu "ispeglanu" listu
        for (Certificate root : rootCerts) {
            List<CertificateRowDTO> chainRows = new ArrayList<>();
            flattenHierarchy(root, 0, certsByIssuer, chainRows);
            allChains.add(new CertificateChainDisplayDTO(chainRows));
        }
        return allChains;
    }

    private void flattenHierarchy(Certificate currentCert, int depth, Map<String, List<Certificate>> certsByIssuer, List<CertificateRowDTO> chainRows) {
        // Dodaj trenutni sertifikat u listu sa njegovom dubinom
        chainRows.add(new CertificateRowDTO(mapCertificateToDTO(currentCert), depth));

        // Pronađi sve sertifikate koje je ovaj sertifikat izdao
        String subjectName = currentCert.getX509Certificate().getSubjectX500Principal().getName();
        List<Certificate> children = certsByIssuer.get(subjectName);

        if (children != null) {
            for (Certificate child : children) {
                // Pazi da ne upadneš u beskonačnu petlju kod self-signed sertifikata
                if (!child.getSerial().equals(currentCert.getSerial())) {
                    flattenHierarchy(child, depth + 1, certsByIssuer, chainRows);
                }
            }
        }
    }

//    public List<IssuingCertificateDTO> getAllIssuingCertificates() {
//        return certificateRepository.findAll().stream()
//                .filter(cert -> cert.getStatus() == CertificateStatus.VALID)
//                .filter(cert -> {
//                    if (cert.getX509Certificate() == null) {
//                        return false;
//                    }
//                    X509Certificate x509 = cert.getX509Certificate();
//
//                    boolean isCa = x509.getBasicConstraints() != -1;
//                    if (!isCa) {
//                        return false;
//                    }
//
//                    try {
//                        x509.checkValidity();
//                        return true;
//                    } catch (CertificateException e) {
//                        return false;
//                    }
//                })
//                .map(cert -> new IssuingCertificateDTO(
//                        cert.getSerial().toString(),
//                        cert.getX509Certificate().getSubjectX500Principal().getName()
//                ))
//                .collect(Collectors.toList());
//    }
    // ja dodala
    public List<IssuingCertificateDTO> getAllIssuingCertificates() {
        return certificateRepository.findAll().stream()
                .filter(cert -> cert.getX509Certificate() != null)
                .filter(cert -> cert.getStatus() == CertificateStatus.VALID)
                .filter(cert -> !isRevoked(cert))
                .filter(cert -> {
                    X509Certificate x509 = cert.getX509Certificate();
                    if (x509.getBasicConstraints() == -1) return false;      // nije CA
                    boolean[] ku = x509.getKeyUsage();
                    if (ku == null || ku.length <= 5 || !ku[5]) return false; // nema keyCertSign
                    try {
                        x509.checkValidity();
                    } catch (CertificateException e) {
                        return false;
                    }
                    return true;
                })
                .map(cert -> new IssuingCertificateDTO(
                        cert.getSerial(),
                        cert.getX509Certificate().getSubjectX500Principal().getName()
                ))
                .sorted(Comparator.comparing(IssuingCertificateDTO::getSubject, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }


//    public List<IssuingCertificateDTO> getIssuingCertificatesForUser(String userId) {
//        User user = userRepository.findByKeycloakId(userId).orElse(null);
//
//        if (user == null) {
//            return Collections.emptyList();
//        }
//
//        Set<Certificate> assignedCertificates = user.getCertificates();
//
//        return assignedCertificates.stream()
//                .filter(cert -> cert.getStatus() == CertificateStatus.VALID)
//                .filter(cert -> {
//                    X509Certificate x509 = cert.getX509Certificate();
//                    if (x509 == null) {
//                        return false;
//                    }
//                    boolean isCa = x509.getBasicConstraints() != -1;
//                    try {
//                        x509.checkValidity(); // Proverava da li je istekao
//                        return isCa;
//                    } catch (CertificateException e) {
//                        return false;
//                    }
//                })
//                .map(cert -> new IssuingCertificateDTO(
//                        cert.getSerial(),
//                        cert.getX509Certificate().getSubjectX500Principal().getName()
//                ))
//                .collect(Collectors.toList());
//    }
    public List<IssuingCertificateDTO> getIssuingCertificatesForUser(String userId) {
        User user = userRepository.findByKeycloakId(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getCertificates().stream()
                .filter(cert -> cert.getStatus() == CertificateStatus.VALID)   // samo važeći
                .filter(cert -> cert.getX509Certificate() != null)             // mora postojati X509 objekat
                .filter(cert -> !isRevoked(cert))
                .filter(cert -> {
                    X509Certificate x509 = cert.getX509Certificate();

                    // mora biti CA sertifikat
                    if (x509.getBasicConstraints() == -1) return false;

                    // mora imati pravo da potpisuje druge (keyCertSign)
                    boolean[] ku = x509.getKeyUsage();
                    if (ku == null || ku.length <= 5 || !ku[5]) return false;

                    // mora biti još uvek važeći (nije istekao)
                    try {
                        x509.checkValidity();
                    } catch (CertificateException e) {
                        return false;
                    }

                    return true;
                })
                .map(cert -> new IssuingCertificateDTO(
                        cert.getSerial(),
                        cert.getX509Certificate().getSubjectX500Principal().getName()
                ))
                .sorted(Comparator.comparing(IssuingCertificateDTO::getSubject, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
    private boolean isCaCert(X509Certificate c) throws Exception {
        byte[] bc = c.getExtensionValue(Extension.basicConstraints.getId());
        if (bc == null) return false;
        BasicConstraints bcExt = BasicConstraints.getInstance(
                JcaX509ExtensionUtils.parseExtensionValue(bc));
        return bcExt.isCA();
    }

    private String pickSigAlg(PrivateKey issuerKey) {
        String alg = issuerKey.getAlgorithm(); // "RSA" ili "EC"
        return "EC".equalsIgnoreCase(alg) ? "SHA256withECDSA" : "SHA256withRSA";
    }

    // za sada stub; koristi se u filtrima/validaciji
    private boolean isRevoked(Certificate cert) {
        return cert.getStatus() == CertificateStatus.REVOKED;
    }

}
