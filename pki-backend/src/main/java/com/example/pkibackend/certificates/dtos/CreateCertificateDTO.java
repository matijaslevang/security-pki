package com.example.pkibackend.certificates.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class CreateCertificateDTO {
    private String issuerSerialNumber;
    private SubjectDTO subjectDto;
    @JsonProperty("selfSigned")
    private boolean selfSigned;
    @JsonProperty("intermediate")
    private boolean intermediate;
    @JsonProperty("skiaki")
    private boolean skiaki;
    private String sanString;
    private Date startDate;
    private Date endDate;
}
