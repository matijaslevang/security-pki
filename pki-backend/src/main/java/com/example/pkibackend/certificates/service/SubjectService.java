package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.SubjectDTO;
import com.example.pkibackend.certificates.model.Subject;
import com.example.pkibackend.certificates.repository.SubjectRepository;
import com.example.pkibackend.util.DTOToX500Name;
import com.example.pkibackend.util.KeyPairGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

@Service
public class SubjectService {
    @Autowired
    private SubjectRepository subjectRepository;

    public Subject createIfNotExist(SubjectDTO subjectDTO) {
        X500Name subjectXName = DTOToX500Name.SubjectDTOToX500Name(subjectDTO);
        Subject subject = findByX500NameString(subjectXName.toString());
        if (subject != null) {
            return subject;
        }
        subject = new Subject();
        subject.setX500Name(subjectXName);
        KeyPair keyPair = KeyPairGenerator.generateKeyPair();
        subject.setPublicKey(keyPair.getPublic());
        return subjectRepository.save(subject);
    }

    public Subject findByX500NameString(String x500NameString) {
        return subjectRepository.findByX500NameString(x500NameString);
    }
}
