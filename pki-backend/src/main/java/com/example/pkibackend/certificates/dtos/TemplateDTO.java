package com.example.pkibackend.certificates.dtos;

import com.example.pkibackend.certificates.model.Template;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TemplateDTO {
    private Long id;
    private String name;
    private String serialNumber;
    private String commonNameRegex;
    private String sanRegex;
    private Integer ttl;
    @JsonProperty("skiakiDefaultValue")
    private Boolean skiakiDefaultValue;
    private List<Boolean> keyUsageDefaultValues;
    private List<Boolean> extKeyUsageDefaultValues;

    public TemplateDTO(Template template) {
        this.id = template.getId();
        this.name = template.getName();
        this.serialNumber = template.getCertificateSerialNumber();
        this.commonNameRegex = template.getCommonNameRegex();
        this.sanRegex = template.getSanRegex();
        this.ttl = template.getTtl();
        this.skiakiDefaultValue = template.getSkiAki();
        keyUsageDefaultValues = new ArrayList<>();
        keyUsageDefaultValues.add(template.getDigitalSignature());
        keyUsageDefaultValues.add(template.getNonRepudiation());
        keyUsageDefaultValues.add(template.getKeyEncipherment());
        keyUsageDefaultValues.add(template.getDataEncipherment());
        keyUsageDefaultValues.add(template.getKeyAgreement());
        keyUsageDefaultValues.add(template.getCRLSign());
        extKeyUsageDefaultValues = new ArrayList<>();
        extKeyUsageDefaultValues.add(template.getServerAuth());
        extKeyUsageDefaultValues.add(template.getClientAuth());
        extKeyUsageDefaultValues.add(template.getCodeSigning());
        extKeyUsageDefaultValues.add(template.getEmailProtection());
        extKeyUsageDefaultValues.add(template.getTimeStamping());
    }
}
