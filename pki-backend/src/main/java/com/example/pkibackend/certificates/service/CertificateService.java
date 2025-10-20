package com.example.pkibackend.certificates.service;
import com.example.pkibackend.users.service.UserService;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import com.example.pkibackend.util.Encryption;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import com.example.pkibackend.certificates.dtos.*;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.certificates.model.enums.CertificateStatus;
import com.example.pkibackend.certificates.model.enums.RevocationReason;
import com.example.pkibackend.certificates.repository.CertificateRepository;
import com.example.pkibackend.certificates.repository.UserRepository;
import com.example.pkibackend.util.BooleanListToKeyUsage;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import com.example.pkibackend.certificates.dtos.CreateCertCsrUploadDTO;
@Service
public class CertificateService {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private IssuerService issuerService;
    @Autowired
    private UserService userService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CrlService crlService;

    @Autowired
    private Encryption encryption;
    @Autowired
    private OrganizationService organizationService;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

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
    private boolean isCa(X509Certificate x) {
        return x.getBasicConstraints() != -1;
    }

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
            contentSigner = builder.build(issuer.getPrivateKey(encryption, organizationService));
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
            if (createCertificateDTO.isSelfSigned() || createCertificateDTO.isIntermediate()) {
                certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
                certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | BooleanListToKeyUsage.getKeyUsageMaskFromBooleanList(createCertificateDTO.getKeyUsageValues())));
            } else {
                certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
                certGen.addExtension(Extension.keyUsage, true, new KeyUsage(BooleanListToKeyUsage.getKeyUsageMaskFromBooleanList(createCertificateDTO.getKeyUsageValues())));
            }

            certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(BooleanListToKeyUsage.getExtKeyUsageMaskFromBooleanList(createCertificateDTO.getExtKeyUsageValues())));

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

        // CDP (CRL Distribution Points)
        String cdpUrl = crlService.getPublicCrlUrl();
        DistributionPointName dpName = new DistributionPointName(
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, cdpUrl)));
        DistributionPoint dp = new DistributionPoint(dpName, null, null);
        CRLDistPoint cdp = new CRLDistPoint(new DistributionPoint[]{dp});
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

        boolean allowed = isAdmin
                || requesterKcUuid.equals(c.getIssuerId())
                || isSubjectOwner(requesterKcUuid, c.getSubjectId());

        //if (!allowed) throw new AccessDeniedException("Forbidden");

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

        PrivateKey privateKey = issuer.getPrivateKey(encryption, organizationService);
        X509Certificate certificate = certificateRecord.getX509Certificate();

        try {
            // 3. Kreiraj novi, prazan PKCS12 KeyStore u memoriji
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // Inicijalizacija praznog keystore-a

            // 4. Kreiraj lanac sertifikata. Za sada, sadrži samo jedan sertifikat.
            // U složenijim scenarijima, ovde bi se dodavao i sertifikat issuera, itd.
            java.security.cert.Certificate[] certificateChain = {certificate};

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

    public CertificateInfoDTO mapCertificateToDTO(Certificate certificate) {
        X509Certificate x509Cert = certificate.getX509Certificate();
        return new CertificateInfoDTO(
                x509Cert.getSerialNumber().toString(),
                x509Cert.getSubjectX500Principal().getName(), // Dobijamo Subject
                x509Cert.getIssuerX500Principal().getName(),  // Dobijamo Issuer
                x509Cert.getNotBefore(), // Važi od
                x509Cert.getNotAfter(),// Važi do
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

                    List<CertificateInfoDTO> issuedCertificates = userService.getLoggedUser().getIssuedCertificates().stream()
                            .map(this::mapCertificateToDTO)
                            .collect(Collectors.toList());

                    return new CertificateChainDTO(caDto, issuedCertificates);
                })
                .collect(Collectors.toList());

        /*return userCaCerts.stream()
                .map(caCert -> {
                    CertificateInfoDTO caDto = mapCertificateToDTO(caCert);

                    List<CertificateInfoDTO> issuedCertificates = allCertificatesInSystem.stream()
                            .filter(cert -> {
                                if (cert.getX509Certificate() == null || cert.getX509Certificate().getIssuerX500Principal() == null)
                                    return false;
                                // Pronalazimo sve sertifikate koje je ovaj CA sertifikat izdao
                                return cert.getX509Certificate().getIssuerX500Principal().getName().equals(caDto.getSubject());
                            })
                            .map(this::mapCertificateToDTO)
                            .collect(Collectors.toList());

                    return new CertificateChainDTO(caDto, issuedCertificates);
                })
                .collect(Collectors.toList());*/
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

    private boolean isRevoked(Certificate cert) {
        return cert.getStatus() == CertificateStatus.REVOKED;
    }

    public Certificate issueFromCsrWithExtensions(CreateCertCsrUploadDTO dto, byte[] csrBytes) {
        // 1) Validacija izdavaoca (isto kao pre, samo koristi podatke iz DTO-a)
        Certificate issuerRecord = certificateRepository
                .findById(dto.getIssuerSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("Issuing certificate not found."));
        X509Certificate issuerX = issuerRecord.getX509Certificate();
        if (issuerRecord.getStatus() == CertificateStatus.REVOKED)
            throw new IllegalArgumentException("Issuing certificate is revoked.");
        if (issuerX.getBasicConstraints() < 0)
            throw new IllegalArgumentException("Issuing certificate is not a CA.");
        boolean[] ku = issuerX.getKeyUsage();
        if (ku == null || ku.length <= 5 || !ku[5])
            throw new IllegalArgumentException("Issuing certificate lacks KeyCertSign.");
        try {
            issuerX.checkValidity();
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Issuing certificate not valid now.");
        }
        if (dto.getEndDate().after(issuerX.getNotAfter()))
            throw new IllegalArgumentException("End date exceeds issuer validity.");
        Issuer issuer = issuerService.getIssuer(issuerRecord.getIssuerId());
        if (issuer == null) throw new IllegalStateException("Issuer owner not found.");

        // 2) Parsiranje CSR-a (potpuno ista logika kao pre)
        org.bouncycastle.pkcs.PKCS10CertificationRequest csr;
        try {
            String text = new String(csrBytes, java.nio.charset.StandardCharsets.US_ASCII);
            if (text.contains("-----BEGIN")) {
                try (org.bouncycastle.util.io.pem.PemReader pr = new org.bouncycastle.util.io.pem.PemReader(new java.io.StringReader(text))) {
                    org.bouncycastle.util.io.pem.PemObject po = pr.readPemObject();
                    csr = new org.bouncycastle.pkcs.PKCS10CertificationRequest(po.getContent());
                }
            } else {
                csr = new org.bouncycastle.pkcs.PKCS10CertificationRequest(csrBytes);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CSR content.");
        }

        // 3) & 4) Ekstrakcija i čuvanje Subject-a (potpuno ista logika kao pre)
        org.bouncycastle.asn1.x500.X500Name subjectX = csr.getSubject();
        java.security.PublicKey subjectPubKey;
        try {
            subjectPubKey = new org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest(csr).getPublicKey();
        } catch (Exception e) {
            throw new IllegalArgumentException("CSR public key extract failed.");
        }
        Subject s = subjectService.findByX500NameString(subjectX.toString());
        if (s == null) {
            s = new Subject();
            s.setX500Name(subjectX);
            s.setPublicKey(subjectPubKey);
            s = subjectService.save(s);
        }

        // 5) Kreiranje sertifikata
        java.math.BigInteger serial;
        do {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            serial = new java.math.BigInteger(uuid.toString().replace("-", ""), 16);
            if (serial.signum() < 0) serial = serial.negate();
        } while (certificateRepository.existsById(serial.toString()));

        org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder certGen =
                new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                        issuer.getX500Name(), serial, dto.getStartDate(), dto.getEndDate(), subjectX, subjectPubKey);

        // 6) *** KLJUČNA IZMENA: Dinamičko dodavanje ekstenzija iz DTO-a ***
        try {
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

            // Basic Constraints (uvek 'false' za end-entity)
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

            // Key Usage (koristi helper klasu i listu iz DTO-a)
            certGen.addExtension(Extension.keyUsage, true, new KeyUsage(BooleanListToKeyUsage.getKeyUsageMaskFromBooleanList(dto.getKeyUsageValues())));

            // Extended Key Usage (koristi helper klasu i listu iz DTO-a)
            certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(BooleanListToKeyUsage.getExtKeyUsageMaskFromBooleanList(dto.getExtKeyUsageValues())));

            // SKI/AKI (na osnovu booleana iz DTO-a)
            if (dto.isSkiaki()) {
                certGen.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(subjectPubKey));
                certGen.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(issuer.getPublicKey()));
            }

            // SAN (na osnovu stringa iz DTO-a)
            if (dto.getSanString() != null && !dto.getSanString().isEmpty()) {
                List<String> sanList = Arrays.stream(dto.getSanString().split(",")).map(String::trim).filter(str -> !str.isEmpty()).toList();
                GeneralName[] sanNameList = sanList.stream().map(name -> new GeneralName(GeneralName.dNSName, name)).toArray(GeneralName[]::new);
                GeneralNames sanNames = new GeneralNames(sanNameList);
                certGen.addExtension(Extension.subjectAlternativeName, false, sanNames);
            }

            // CDP (uvek dodajemo)
            String cdpUrl = crlService.getPublicCrlUrl();
            DistributionPointName dpName = new DistributionPointName(new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, cdpUrl)));
            DistributionPoint dp = new DistributionPoint(dpName, null, null);
            CRLDistPoint cdp = new CRLDistPoint(new DistributionPoint[]{dp});
            certGen.addExtension(Extension.cRLDistributionPoints, false, cdp);

        } catch (Exception e) { // Hvata CertIOException i NoSuchAlgorithmException
            throw new RuntimeException("Failed to add extensions to certificate.", e);
        }

        // 7) Potpisivanje i čuvanje (potpuno ista logika kao pre)
        String sigAlg = pickSigAlg(issuer.getPrivateKey(encryption, organizationService));
        ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(issuer.getPrivateKey(encryption, organizationService));
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }
        org.bouncycastle.cert.X509CertificateHolder holder = certGen.build(signer);
        X509Certificate cert;
        try {
            cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
        Certificate wrap = new Certificate();
        wrap.setSerial(serial.toString());
        wrap.setSubjectId(s.getId());
        wrap.setIssuerId(issuer.getUserUUID());
        wrap.setX509Certificate(cert);
        wrap.setStatus(CertificateStatus.VALID);
        return certificateRepository.save(wrap);
    }

    public Certificate issueFromCsr(String issuerSerialNumber,
                                    Instant start, Instant end,
                                    byte[] csrBytes) {

        // 1) Nađi i validiraj CA izdavaoca
        Certificate issuerRecord = certificateRepository
                .findById(issuerSerialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Issuing certificate not found."));
        X509Certificate issuerX = issuerRecord.getX509Certificate();
        if (issuerRecord.getStatus() == CertificateStatus.REVOKED)
            throw new IllegalArgumentException("Issuing certificate is revoked.");
        if (issuerX.getBasicConstraints() < 0)
            throw new IllegalArgumentException("Issuing certificate is not a CA.");
        boolean[] ku = issuerX.getKeyUsage();
        if (ku == null || ku.length <= 5 || !ku[5])
            throw new IllegalArgumentException("Issuing certificate lacks KeyCertSign.");
        try {
            issuerX.checkValidity();
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Issuing certificate not valid now.");
        }
        if (Date.from(end).after(issuerX.getNotAfter()))
            throw new IllegalArgumentException("End date exceeds issuer validity.");

        Issuer issuer = issuerService.getIssuer(issuerRecord.getIssuerId());
        if (issuer == null) throw new IllegalStateException("Issuer owner not found.");

        // 2) Parse CSR (PEM ili DER)
        org.bouncycastle.pkcs.PKCS10CertificationRequest csr;
        try {
            String text = new String(csrBytes, java.nio.charset.StandardCharsets.US_ASCII);
            if (text.contains("-----BEGIN")) {
                try (org.bouncycastle.util.io.pem.PemReader pr =
                             new org.bouncycastle.util.io.pem.PemReader(new java.io.StringReader(text))) {
                    org.bouncycastle.util.io.pem.PemObject po = pr.readPemObject();
                    csr = new org.bouncycastle.pkcs.PKCS10CertificationRequest(po.getContent());
                }
            } else {
                csr = new org.bouncycastle.pkcs.PKCS10CertificationRequest(csrBytes);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CSR content.");
        }

        // 3) Izvuci subject i public key iz CSR-a
        org.bouncycastle.asn1.x500.X500Name subjectX = csr.getSubject();
        java.security.PublicKey subjectPubKey;
        try {
            subjectPubKey = new org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest(csr).getPublicKey();
        } catch (Exception e) {
            throw new IllegalArgumentException("CSR public key extract failed.");
        }

        // 4) Persistuj/nađi Subject
        Subject s = subjectService.findByX500NameString(subjectX.toString());
        if (s == null) {
            s = new Subject();
            s.setX500Name(subjectX);
            s.setPublicKey(subjectPubKey);
            s = subjectService.save(s); // dodaj public save u SubjectService ako ga nemaš
        }

        // 5) Napravi cert kao i do sada, ali koristi subject iz CSR-a
        java.math.BigInteger serial;
        do {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            serial = new java.math.BigInteger(uuid.toString().replace("-", ""), 16);
            if (serial.signum() < 0) serial = serial.negate();
        } while (certificateRepository.existsById(serial.toString()));

        org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder certGen =
                new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                        issuer.getX500Name(),
                        serial,
                        Date.from(start),
                        Date.from(end),
                        subjectX,
                        subjectPubKey
                );

        try {
            // basicConstraints za EE
            certGen.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                    new org.bouncycastle.asn1.x509.BasicConstraints(false));
            // keyUsage tipično za EE
            certGen.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                    new org.bouncycastle.asn1.x509.KeyUsage(
                            org.bouncycastle.asn1.x509.KeyUsage.digitalSignature |
                                    org.bouncycastle.asn1.x509.KeyUsage.keyEncipherment));

            // SKI/AKI
            JcaX509ExtensionUtils ext = null;
            try {
                ext = new JcaX509ExtensionUtils();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            certGen.addExtension(org.bouncycastle.asn1.x509.Extension.subjectKeyIdentifier, false,
                    ext.createSubjectKeyIdentifier(subjectPubKey));
            certGen.addExtension(org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier, false,
                    ext.createAuthorityKeyIdentifier(issuer.getPublicKey()));

            // CDP iz CrlService
            String cdpUrl = crlService.getPublicCrlUrl();
            org.bouncycastle.asn1.x509.DistributionPointName dpName = new org.bouncycastle.asn1.x509.DistributionPointName(
                    new org.bouncycastle.asn1.x509.GeneralNames(
                            new org.bouncycastle.asn1.x509.GeneralName(org.bouncycastle.asn1.x509.GeneralName.uniformResourceIdentifier, cdpUrl)));
            org.bouncycastle.asn1.x509.DistributionPoint dp = new org.bouncycastle.asn1.x509.DistributionPoint(dpName, null, null);
            org.bouncycastle.asn1.x509.CRLDistPoint cdp =
                    new org.bouncycastle.asn1.x509.CRLDistPoint(new org.bouncycastle.asn1.x509.DistributionPoint[]{dp});
            certGen.addExtension(org.bouncycastle.asn1.x509.Extension.cRLDistributionPoints, false, cdp);
        } catch (org.bouncycastle.cert.CertIOException e) {
            throw new RuntimeException(e);
        }

        String sigAlg = pickSigAlg(issuer.getPrivateKey(encryption, organizationService));
        ContentSigner signer = null;
        try {
            signer = new JcaContentSignerBuilder(sigAlg)
                    .setProvider("BC").build(issuer.getPrivateKey(encryption, organizationService));
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }
        org.bouncycastle.cert.X509CertificateHolder holder = certGen.build(signer);
        X509Certificate cert;
        try {
            cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        Certificate wrap = new Certificate();
        wrap.setSerial(serial.toString());
        wrap.setSubjectId(s.getId());
        wrap.setIssuerId(issuer.getUserUUID());
        wrap.setX509Certificate(cert);
        wrap.setStatus(CertificateStatus.VALID);

        return certificateRepository.save(wrap);
    }

    public byte[] createEndEntityWithKeystore(CreateCertificateDTO dto, String password) {
        // 1. Validate as in createCertificate
        if (dto.getIssuerSerialNumber() == null || dto.getIssuerSerialNumber().isEmpty())
            throw new IllegalArgumentException("Issuer serial number is required.");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Password is required.");

        Certificate issuerRecord = certificateRepository.findById(dto.getIssuerSerialNumber())
                .orElseThrow(() -> new RuntimeException("Issuing certificate not found."));
        Issuer issuer = issuerService.getIssuer(issuerRecord.getIssuerId());
        if (issuer == null) throw new RuntimeException("Issuer owner not found.");

        // 2. Generate key pair
        KeyPair kp;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
            kpg.initialize(2048, new SecureRandom());
            kp = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Key pair generation failed", e);
        }
        PublicKey pub = kp.getPublic();
        PrivateKey priv = kp.getPrivate();

        // 3. Build subject name from DTO
        X500Name subjectName = buildSubjectName(dto.getSubjectDto());

        // 4. Create and save subject (public key only)
        Subject subject = new Subject();
        subject.setX500Name(subjectName);
        subject.setPublicKey(pub);
        subject = subjectService.save(subject);

        // 5. Generate serial
        BigInteger serial;
        do {
            UUID uuid = UUID.randomUUID();
            serial = new BigInteger(uuid.toString().replace("-", ""), 16);
            if (serial.signum() < 0) serial = serial.negate();
        } while (certificateRepository.existsById(serial.toString()));

        // 6. Build certificate
        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                issuer.getX500Name(),
                serial,
                dto.getStartDate(),
                dto.getEndDate(),
                subjectName,
                pub
        );

        // Add extensions (basicConstraints false, keyUsage, extKeyUsage, SKI/AKI if selected, SAN, CDP)
        // Assume addExtensions method exists from your createCertificate logic
        // addExtensions(certGen, dto, issuer, pub);

        // Sign
        ContentSigner contentSigner;
        try {
            contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(issuer.getPrivateKey(encryption, organizationService));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        X509CertificateHolder certHolder = certGen.build(contentSigner);
        X509Certificate cert;
        try {
            cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        // Save certificate
        Certificate certificateWrapper = new Certificate();
        certificateWrapper.setSerial(serial.toString());
        certificateWrapper.setSubjectId(subject.getId());
        certificateWrapper.setIssuerId(issuer.getUserUUID());
        certificateWrapper.setX509Certificate(cert);
        certificateWrapper.setStatus(CertificateStatus.VALID);
        certificateWrapper = certificateRepository.save(certificateWrapper);

        User user = userService.getLoggedUser();
        user.getIssuedCertificates().add(certificateWrapper);
        userService.save(user);

        // 7. Create PKCS12 keystore (private key not saved)
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            java.security.cert.Certificate[] chain = { cert };
            ks.setKeyEntry(serial.toString(), priv, password.toCharArray(), chain);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ks.store(baos, password.toCharArray());
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Keystore generation failed", e);
        }
    }
    private X500Name buildSubjectName(SubjectDTO dto) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        if (dto.getCommonName() != null) builder.addRDN(BCStyle.CN, dto.getCommonName());
        if (dto.getGivenName() != null) builder.addRDN(BCStyle.GIVENNAME, dto.getGivenName());
        if (dto.getSurname() != null) builder.addRDN(BCStyle.SURNAME, dto.getSurname());
        if (dto.getOrganization() != null) builder.addRDN(BCStyle.O, dto.getOrganization());
        if (dto.getDepartment() != null) builder.addRDN(BCStyle.OU, dto.getDepartment());
        if (dto.getCountry() != null) builder.addRDN(BCStyle.C, dto.getCountry());
        if (dto.getEmail() != null) builder.addRDN(BCStyle.E, dto.getEmail());
        return builder.build();
    }

}
