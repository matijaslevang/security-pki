package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.dtos.AssignCertificateRequestDTO;
import com.example.pkibackend.certificates.dtos.UserDTO;
import com.example.pkibackend.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_admin-user')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{userId}/assign-certificate")
    @PreAuthorize("hasAuthority('ROLE_admin-user')")
    public ResponseEntity<?> assignCertificate(
            @PathVariable Integer userId,
            @RequestBody AssignCertificateRequestDTO request) {
        try {
            userService.assignCertificateToUser(userId, request.getSerialNumber());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
