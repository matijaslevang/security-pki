package com.example.pkibackend.certificates.repository;

import com.example.pkibackend.certificates.model.Issuer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssuerRepository extends JpaRepository<Issuer, String> {
}
