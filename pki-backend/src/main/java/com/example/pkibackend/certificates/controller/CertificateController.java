package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.dtos.*;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.model.User;
import com.example.pkibackend.certificates.service.CertificateService;
import com.example.pkibackend.certificates.service.IssuerService;
import com.example.pkibackend.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(value="/end-entity", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> createEndEntityCertificate(@RequestBody CreateCertificateDTO createCertificateDTO) {
        Certificate certificate = certificateService.createCertificate(createCertificateDTO, null);

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
            if (certificate == null) {
                return new ResponseEntity<>("Failed to create certificate.", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(true, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{serialNumber}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadCertificateAsKeyStore(
            @PathVariable BigInteger serialNumber,
            Principal principal) { // Spring Security će automatski ubaciti ulogovanog korisnika

        try {
            // principal.getName() će vratiti 'sub' claim iz JWT tokena (korisnički UUID)
            String userIdAsPassword = principal.getName();

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

            List<CertificateInfoDTO> certificates = certificateService.getCertificatesForUser(subjectId);
            return new ResponseEntity<>(certificates, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
}
