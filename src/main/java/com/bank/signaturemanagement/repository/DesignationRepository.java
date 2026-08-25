package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    List<Designation> findAllByOrderByDesignationNameAsc();

    Optional<Designation> findByDesignationName(String designationName);

    boolean existsByDesignationName(String designationName);
}