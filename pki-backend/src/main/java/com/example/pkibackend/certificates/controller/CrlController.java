package com.example.pkibackend.certificates.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crl")
public class CrlController {
    @GetMapping(value="/latest", produces="application/pkix-crl")
    public ResponseEntity<byte[]> latest() {
        // TODO: generiši CRL iz certificateRepository gde status == REVOKED
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}