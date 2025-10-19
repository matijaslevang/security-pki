package com.example.pkibackend.certificates.dtos;

import java.util.Date;

public class CsrUploadDTO {
    private String pem;               // PEM CSR
    private String issuerSerial;      // serial CA koji potpisuje
    private Date notBefore;
    private Date notAfter;

    public String getPem() { return pem; }
    public void setPem(String pem) { this.pem = pem; }
    public String getIssuerSerial() { return issuerSerial; }
    public void setIssuerSerial(String issuerSerial) { this.issuerSerial = issuerSerial; }
    public Date getNotBefore() { return notBefore; }
    public void setNotBefore(Date notBefore) { this.notBefore = notBefore; }
    public Date getNotAfter() { return notAfter; }
    public void setNotAfter(Date notAfter) { this.notAfter = notAfter; }
}