package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Designation;
import com.bank.signaturemanagement.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationService {

    private final DesignationRepository designationRepository;

    public List<Designation> findAll() {
        return designationRepository.findAllByOrderByDesignationNameAsc();
    }
}
