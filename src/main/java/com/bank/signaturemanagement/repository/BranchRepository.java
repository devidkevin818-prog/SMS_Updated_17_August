package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findAllByOrderByBranchNameAsc();

    Optional<Branch> findByBranchName(String branchName);

    boolean existsByBranchName(String branchName);
}