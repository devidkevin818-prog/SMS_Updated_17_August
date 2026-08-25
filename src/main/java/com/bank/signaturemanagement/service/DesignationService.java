package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Designation;
import com.bank.signaturemanagement.repository.DesignationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DesignationService {

    private final DesignationRepository designationRepository;

    public DesignationService(DesignationRepository designationRepository) {
        this.designationRepository = designationRepository;
    }

    public List<Designation> findAll() {
        return designationRepository.findAllByOrderByDesignationNameAsc();
    }
}
