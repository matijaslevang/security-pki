package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.model.Organization;
import com.example.pkibackend.certificates.repository.OrganizationRepository;
import com.example.pkibackend.util.Encryption;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private Encryption encryption;

    public Organization createIfNotExist(String name) {
        Organization organization = getByName(name);
        if (organization != null) {
            return organization;
        }
        organization = new Organization(name, encryption);
        return organizationRepository.save(organization);
    }

    private Organization getByName(String name) {
        return organizationRepository.findByName(name);
    }
}
