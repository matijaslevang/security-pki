package com.example.pkibackend.certificates.service;

import com.example.pkibackend.certificates.dtos.TemplateCreateDTO;
import com.example.pkibackend.certificates.model.Template;
import com.example.pkibackend.certificates.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    public Template getTemplate(Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    public Template createTemplate(TemplateCreateDTO templateCreateDTO) {
        Template template = new Template(templateCreateDTO);
        return templateRepository.save(template);
    }

    public List<Template> getAllByCertificateSerialNumber(String serial) {
        return templateRepository.findAllByCertificateSerialNumber(serial);
    }

}
