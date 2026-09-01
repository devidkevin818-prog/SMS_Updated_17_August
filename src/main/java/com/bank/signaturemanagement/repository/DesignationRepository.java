package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    List<Designation> findAllByOrderByDesignationNameAsc();

    List<Designation> findByIsActiveTrueOrderByDesignationNameAsc();

    Optional<Designation> findByDesignationName(String designationName);

    boolean existsByDesignationName(String designationName);

    boolean existsByDesignationNameIgnoreCaseAndDesignationIdNot(String designationName, Long designationId);

    boolean existsByDesignationNameIgnoreCase(String designationName);
}
