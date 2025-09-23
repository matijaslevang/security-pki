package com.example.pkibackend.certificates.controller;

import com.example.pkibackend.certificates.dtos.KcUserDto;
import com.example.pkibackend.certificates.dtos.UserDTO;
import com.example.pkibackend.certificates.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/iam/users")
@RequiredArgsConstructor
public class KeycloakUserController {
    private final KeycloakUserService svc;

    // GET /iam/users?roles=admin,ca
    @GetMapping("/iam/users")
    public List<KcUserDto> byRoles(@RequestParam List<String> roles) {
        return svc.usersWithAnyRole(roles); // prosleđuje ["admin-user","ca-user"]
    }

}
