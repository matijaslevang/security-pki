package com.example.pkibackend.certificates.repository;

import com.example.pkibackend.certificates.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, BigInteger> {
    Certificate findByIssuerIdAndSubjectId(String issuerId, Long subjectId);

    List<Certificate> findBySubjectId(Long subjectId);

    List<Certificate> findByIssuerId(String userId);
}
