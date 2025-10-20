package com.example.pkibackend.certificates.repository;

import com.example.pkibackend.certificates.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Organization findByName(String name);
}
