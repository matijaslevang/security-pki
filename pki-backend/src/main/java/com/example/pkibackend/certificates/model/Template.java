package com.example.pkibackend.certificates.model;

import com.example.pkibackend.certificates.dtos.TemplateCreateDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "templates")
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String certificateSerialNumber;

    @Column(nullable = false)
    private String commonNameRegex;

    @Column(nullable = false)
    private String sanRegex;

    @Column(nullable = false)
    private Integer ttl;

    @Column(nullable = false)
    private Boolean skiAki;

    @Column(nullable = false)
    private Boolean digitalSignature;

    @Column(nullable = false)
    private Boolean nonRepudiation;

    @Column(nullable = false)
    private Boolean keyEncipherment;

    @Column(nullable = false)
    private Boolean dataEncipherment;

    @Column(nullable = false)
    private Boolean keyAgreement;

    @Column(nullable = false)
    private Boolean cRLSign;

    @Column(nullable = false)
    private Boolean serverAuth;

    @Column(nullable = false)
    private Boolean clientAuth;

    @Column(nullable = false)
    private Boolean codeSigning;

    @Column(nullable = false)
    private Boolean emailProtection;

    @Column(nullable = false)
    private Boolean timeStamping;

    public Template(TemplateCreateDTO templateCreateDTO) {
        this.name = templateCreateDTO.getName();
        this.certificateSerialNumber = templateCreateDTO.getSerialNumber();
        this.commonNameRegex = templateCreateDTO.getCommonNameRegex();
        this.sanRegex = templateCreateDTO.getSanRegex();
        this.skiAki = templateCreateDTO.getSkiakiDefaultValue();
        this.ttl = templateCreateDTO.getTtl();

        List<Boolean> keyUsageDefaultValues = templateCreateDTO.getKeyUsageDefaultValues();
        this.digitalSignature = keyUsageDefaultValues.get(0);
        this.nonRepudiation = keyUsageDefaultValues.get(1);
        this.keyEncipherment = keyUsageDefaultValues.get(2);
        this.dataEncipherment = keyUsageDefaultValues.get(3);
        this.keyAgreement = keyUsageDefaultValues.get(4);
        this.cRLSign = keyUsageDefaultValues.get(5);

        List<Boolean> extKeyUsageDefaultValues = templateCreateDTO.getExtKeyUsageDefaultValues();
        this.serverAuth = extKeyUsageDefaultValues.get(0);
        this.clientAuth = extKeyUsageDefaultValues.get(1);
        this.codeSigning = extKeyUsageDefaultValues.get(2);
        this.emailProtection = extKeyUsageDefaultValues.get(3);
        this.timeStamping = extKeyUsageDefaultValues.get(4);
    }
}
