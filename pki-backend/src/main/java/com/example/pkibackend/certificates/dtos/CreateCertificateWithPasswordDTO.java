package com.example.pkibackend.certificates.dtos;

public class CreateCertificateWithPasswordDTO {
    private CreateCertificateDTO certificate;
    private String password;

    // Getteri i seteri
    public CreateCertificateDTO getCertificate() {
        return certificate;
    }

    public void setCertificate(CreateCertificateDTO certificate) {
        this.certificate = certificate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}