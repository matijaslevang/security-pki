package com.example.pkibackend.util;

import com.example.pkibackend.certificates.dtos.IssuerDTO;
import com.example.pkibackend.certificates.dtos.SubjectDTO;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;

public class DTOToX500Name {
    public static X500Name IssuerDTOToX500Name(IssuerDTO issuerDTO) {
        X500NameBuilder builder = new X500NameBuilder();
        builder.addRDN(BCStyle.CN, issuerDTO.getCommonName());
        builder.addRDN(BCStyle.SURNAME, issuerDTO.getSurname());
        builder.addRDN(BCStyle.GIVENNAME, issuerDTO.getGivenName());
        builder.addRDN(BCStyle.O, issuerDTO.getOrganization());
        builder.addRDN(BCStyle.OU, issuerDTO.getDepartment());
        builder.addRDN(BCStyle.C, issuerDTO.getCountry());
        builder.addRDN(BCStyle.E, issuerDTO.getEmail());
        builder.addRDN(BCStyle.UID, issuerDTO.getUuid());
        return builder.build();
    }

    public static X500Name SubjectDTOToX500Name(SubjectDTO subjectDTO) {
        X500NameBuilder builder = new X500NameBuilder();
        builder.addRDN(BCStyle.CN, subjectDTO.getCommonName());
        builder.addRDN(BCStyle.SURNAME, subjectDTO.getSurname());
        builder.addRDN(BCStyle.GIVENNAME, subjectDTO.getGivenName());
        builder.addRDN(BCStyle.O, subjectDTO.getOrganization());
        builder.addRDN(BCStyle.OU, subjectDTO.getDepartment());
        builder.addRDN(BCStyle.C, subjectDTO.getCountry());
        builder.addRDN(BCStyle.E, subjectDTO.getEmail());
        //builder.addRDN(BCStyle.UID, subjectDTO.getUuid());
        return builder.build();
    }
}
