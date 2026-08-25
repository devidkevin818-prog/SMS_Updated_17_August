package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Branch;
import com.bank.signaturemanagement.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<Branch> findAll() {
        return branchRepository.findAll();
    }
}