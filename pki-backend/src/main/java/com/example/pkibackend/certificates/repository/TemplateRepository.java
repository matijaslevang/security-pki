package com.example.pkibackend.certificates.repository;

import com.example.pkibackend.certificates.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findAllByCertificateSerialNumber(String certificateSerialNumber);
}
