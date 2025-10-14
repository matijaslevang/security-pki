package com.example.pkibackend.certificates.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TemplateCreateDTO {
    private String serialNumber;
    private String commonNameRegex;
    private String sanRegex;
    private Integer ttl;
    private Boolean skiakiDefaultValue;
    private List<Boolean> keyUsageDefaultValues;
    private List<Boolean> extKeyUsageDefaultValues;
}
