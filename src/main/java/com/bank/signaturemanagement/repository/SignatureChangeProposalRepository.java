package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.SignatureChangeProposal; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface SignatureChangeProposalRepository extends JpaRepository<SignatureChangeProposal,Long>{
 @EntityGraph(attributePaths={"employee","employee.designation","initiatedBy"}) List<SignatureChangeProposal> findByStatusOrderByCreatedAtAsc(String status);
 boolean existsByEmployeeIdAndSignatureTypeAndStatusIn(Long employeeId,String signatureType,Collection<String> statuses);
}
