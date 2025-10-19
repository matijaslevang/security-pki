package com.example.pkibackend.certificates.dtos;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data // Lombok za gettere, settere, itd.
public class CreateCertCsrUploadDTO {
    private String issuerSerialNumber;
    private Date startDate;
    private Date endDate;
    private boolean skiaki;
    private String sanString;
    private List<Boolean> keyUsageValues;
    private List<Boolean> extKeyUsageValues;
}