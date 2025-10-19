package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.service.CrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crl")
public class CrlController {
    @Autowired
    private CrlService crlService;
    @GetMapping(value="/latest", produces="application/pkix-crl")
    public ResponseEntity<byte[]> latest() {
        // Pozovi servis da ti da keširanu ili sveže generisanu CRL listu
        byte[] crlDer = crlService.getLatestCrlDer();

        if (crlDer == null || crlDer.length == 0) {
            return ResponseEntity.noContent().build();
        }

        // Vrati CRL kao fajl
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"latest.crl\"")
                .body(crlDer);
    }
}