package com.example.pkibackend.certificates.repository;

import com.example.pkibackend.certificates.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Subject findByX500NameString(String x500NameString);
}
