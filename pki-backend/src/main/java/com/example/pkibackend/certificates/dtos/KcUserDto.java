package com.example.pkibackend.certificates.dtos;

public record KcUserDto(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {}
