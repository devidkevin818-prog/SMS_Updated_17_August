package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.SignatureVersion; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface SignatureVersionRepository extends JpaRepository<SignatureVersion,Long>{
 @EntityGraph(attributePaths={"employee","submittedBy","changeProposal"}) List<SignatureVersion> findByStatusOrderBySubmittedAtAsc(String status);
 List<SignatureVersion> findByEmployeeNumberOrderBySignatureTypeAscVersionNumberDesc(String employeeNumber);
 Optional<SignatureVersion> findByEmployeeNumberAndSignatureTypeAndCurrentApprovedTrue(String employeeNumber,String signatureType);
 long countByEmployeeNumberAndSignatureType(String employeeNumber,String signatureType);
 boolean existsByEmployeeNumberAndSignatureTypeAndStatusIn(String employeeNumber,String signatureType,Collection<String> statuses);
 @Modifying @Query("update SignatureVersion s set s.currentApproved=false where s.employeeNumber=:number and s.signatureType=:type and s.currentApproved=true") void clearCurrent(@Param("number")String number,@Param("type")String type);
}
