package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.UserCreationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCreationRequestRepository
        extends JpaRepository<UserCreationRequest, Long> {

 @EntityGraph(attributePaths = {
         "proposedBy",
         "proposedRole"
 })
 List<UserCreationRequest> findByStatusOrderByCreatedAtAsc(
         String status
 );

 boolean existsByProposedUsernameAndStatusIn(
         String username,
         List<String> statuses
 );

 boolean existsByProposedEmployeeNumberAndStatusIn(
         String employeeNumber,
         List<String> statuses
 );

 boolean existsByProposedEmailIgnoreCaseAndStatusIn(
         String email,
         List<String> statuses
 );
}