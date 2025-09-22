package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.dtos.CreateCertificateDTO;
import com.example.pkibackend.certificates.dtos.IssuerDTO;
import com.example.pkibackend.certificates.dtos.SubjectDTO;
import com.example.pkibackend.certificates.model.Certificate;
import com.example.pkibackend.certificates.model.Issuer;
import com.example.pkibackend.certificates.service.CertificateService;
import com.example.pkibackend.certificates.service.IssuerService;
import com.example.pkibackend.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/certificates")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private UserService userService;

    @Autowired
    private IssuerService issuerService;

    // TODO: PreAuthorize only admin
    @PostMapping(value="/selfsigned", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> createSelfSignedCertificate(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateCertificateDTO createCertificateDTO) {
        IssuerDTO issuerDTO = userService.getIssuer(jwt);
        issuerService.createIfNotExist(issuerDTO);

        // because no X500Name forms should be filled for a self-signed certificate,
        // meaning the SubjectDTO will come empty
        createCertificateDTO.setSubjectDTO(new SubjectDTO(issuerDTO));

        Certificate certificate = certificateService.createCertificate(createCertificateDTO);

        if (certificate == null) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(true, HttpStatus.CREATED);
    }
    @PostMapping(value="/end-entity", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> createEndEntityCertificate(@RequestBody CreateCertificateDTO createCertificateDTO) {
        Certificate certificate = certificateService.createCertificate(createCertificateDTO);

        if (certificate == null) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(true, HttpStatus.CREATED);
    }
}
