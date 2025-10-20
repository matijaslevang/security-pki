package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.dtos.*;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.Template;
import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.certificates.service.CertificateService;
import com.example.pkibackend.certificates.service.CrlService;
import com.example.pkibackend.certificates.service.IssuerService;
import com.example.pkibackend.certificates.service.TemplateService;
import com.example.pkibackend.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("api/certificates")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private UserService userService;

    @Autowired
    private IssuerService issuerService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private CrlService crlService;

    @PostMapping(value="/selfsigned", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_admin-user')")
    public ResponseEntity<Boolean> createSelfSignedCertificate(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateCertificateDTO createCertificateDTO) {
        Issuer issuer = issuerService.createIfNotExistForSelfSigned(createCertificateDTO.getSubjectDto());

        // because no X500Name forms should be filled for a self-signed certificate,
        // meaning the SubjectDTO will come empty

        Certificate certificate = certificateService.createCertificate(createCertificateDTO, issuer);

        if (certificate == null) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(true, HttpStatus.CREATED);
    }
    @PostMapping(value="/end-entity-blob", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_ca-user') or hasAuthority('ROLE_normal-user')")
    public ResponseEntity<byte[]> createEndEntityCertificateBlob(
            @RequestBody CreateCertificateDTO createCertificateDTO,
            @RequestParam("password") String password) {
        try {
            byte[] keystoreBytes = certificateService.createEndEntityWithKeystore(createCertificateDTO, password);
            if (keystoreBytes == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "x-pkcs12"));
            headers.setContentDisposition(ContentDisposition.attachment().filename("certificate.p12").build());
            headers.setContentLength(keystoreBytes.length);
            return new ResponseEntity<>(keystoreBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value="/end-entity", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_ca-user') or hasAuthority('ROLE_normal-user')")
    public ResponseEntity<Boolean> createEndEntityCertificate(@RequestBody CreateCertificateDTO createCertificateDTO) {
        Certificate certificate = certificateService.createCertificate(createCertificateDTO);
        userService.addToCertList(userService.getLoggedUser(), certificate);
        if (certificate == null) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(true, HttpStatus.CREATED);
    }

    @PostMapping(value="/intermediate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_ca-user')")
    public ResponseEntity<?> createIntermediateCertificate(@RequestBody CreateCertificateDTO createCertificateDTO) {
        try {
            Certificate certificate = certificateService.createCertificate(createCertificateDTO);
            userService.addToCertList(userService.getLoggedUser(), certificate);
            if (certificate == null) {
                return new ResponseEntity<>("Failed to create certificate.", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(true, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{serial}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revoke(@PathVariable String serial,
                                    @RequestBody RevocationDTO body,
                                    org.springframework.security.core.Authentication auth,
                                    Principal p) {
        try {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_admin-user".equals(a.getAuthority()));
            certificateService.revoke(serial, body.getReason(), p.getName(), isAdmin);
            return ResponseEntity.ok().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body("Not allowed");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping(value = "/crl/latest", produces = "application/pkix-crl")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> getLatestCrl() {
        byte[] der = crlService.getLatestCrlDer();
        if (der == null || der.length == 0) return ResponseEntity.noContent().build();
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"crl.der\"")
                .body(der);
    }
    @GetMapping("/{serialNumber}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadCertificateAsKeyStore(
            @PathVariable String serialNumber,
            Principal principal) { // Spring Security će automatski ubaciti ulogovanog korisnika

        try {
            // principal.getName() će vratiti 'sub' claim iz JWT tokena (korisnički UUID)

            String userIdAsPassword = "password";

            byte[] keyStoreBytes = certificateService.getCertificateAsKeyStore(serialNumber, userIdAsPassword);

            String filename = serialNumber + ".p12";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/x-pkcs12"));
            headers.setContentDispositionFormData(filename, filename);
            headers.setContentLength(keyStoreBytes.length);

            return new ResponseEntity<>(keyStoreBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/my-certificates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CertificateInfoDTO>> getMyCertificates(Principal principal) {
        try {
            User user = this.userService.getLoggedUser();
            Long subjectId = user.getId().longValue();

            String userId = principal.getName(); // Ovo je ispravan UUID, npr. "6ab46a40-..."

            List<CertificateInfoDTO> certificates = user.getIssuedCertificates().stream().map(certificateService::mapCertificateToDTO).toList();
            return new ResponseEntity<>(certificates, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/csr/upload-extension", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadCsrWithExtensions(
            @RequestPart("dto") CreateCertCsrUploadDTO dto, // Očekuje JSON objekat pod ključem "dto"
            @RequestPart("csr") org.springframework.web.multipart.MultipartFile csrFile) {
        try {
            // Pozivamo novu servisnu metodu
            certificateService.issueFromCsrWithExtensions(dto, csrFile.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(true);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Dobra praksa je logovati ceo stack trace radi lakšeg debagovanja
            e.printStackTrace();
            return ResponseEntity.status(500).body("CSR issue failed: " + e.getMessage());
        }
    }
    @PostMapping(value = "/csr/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadCsr(
            @RequestParam("issuerSerialNumber") String issuerSerialNumber,
            @RequestParam("startDate") String startDateIso,
            @RequestParam("endDate") String endDateIso,
            @RequestPart("csr") org.springframework.web.multipart.MultipartFile csrFile) {
        try {
            java.time.Instant start = java.time.Instant.parse(startDateIso);
            java.time.Instant end = java.time.Instant.parse(endDateIso);

            certificateService.issueFromCsr(issuerSerialNumber, start, end, csrFile.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(true);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("CSR issue failed");
        }
    }

    @GetMapping("/my-chains")
    @PreAuthorize("hasAuthority('ROLE_ca-user')")
    public ResponseEntity<List<CertificateChainDTO>> getMyCertificateChains(Principal principal) {
        try {
            // JEDINI ID KOJI TI TREBA JE IZ PRINCIPALA (TOKENA)
            String userId = principal.getName(); // Ovo je ispravan UUID, npr. "6ab46a40-..."

            // POZOVI SERVIS SA ISPRAVNIM ID-em
            List<CertificateChainDTO> chains = certificateService.getCertificateChainsForCaUser(userId);

            return new ResponseEntity<>(chains, HttpStatus.OK);
        } catch (Exception e) {
            // SAVET ZA BUDUĆNOST: UVEK LOGUJ GREŠKU U CATCH BLOKU!
            // Ovo će ti ispisati tačan uzrok greške u konzoli i uštedeti sate debugovanja.
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/all-chains")
    @PreAuthorize("hasAuthority('ROLE_admin-user')")
    public ResponseEntity<List<CertificateChainDisplayDTO>> getAllCertificateChains() {
        List<CertificateChainDisplayDTO> chains = certificateService.getCertificateChainsForAdmin();
        return new ResponseEntity<>(chains, HttpStatus.OK);
    }

    @GetMapping("/all-issuing-certificates")
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_normal-user')")
    public ResponseEntity<List<IssuingCertificateDTO>> getAllIssuingCertificates() {
        try {
            List<IssuingCertificateDTO> certificates = certificateService.getAllIssuingCertificates();
            return new ResponseEntity<>(certificates, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/template")
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_ca-user')")
    public ResponseEntity<?> createTemplate(@RequestBody TemplateCreateDTO templateDTO) {
        templateService.createTemplate(templateDTO);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/templates/{serial}")
    @PreAuthorize("hasAuthority('ROLE_admin-user') or hasAuthority('ROLE_ca-user')")
    public ResponseEntity<List<TemplateDTO>> getAllTemplatesForSerial(@PathVariable String serial) {
        List<Template> templates = templateService.getAllByCertificateSerialNumber(serial);
        List<TemplateDTO> dtos = templates.stream().map(TemplateDTO::new).toList();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @GetMapping("/my-issuing-certificates")
    @PreAuthorize("hasAuthority('ROLE_ca-user')")
    public ResponseEntity<List<IssuingCertificateDTO>> getMyIssuingCertificates(Principal principal) {
        try {
            String userId = principal.getName();
            List<IssuingCertificateDTO> certificates = certificateService.getIssuingCertificatesForUser(userId);
            return new ResponseEntity<>(certificates, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
