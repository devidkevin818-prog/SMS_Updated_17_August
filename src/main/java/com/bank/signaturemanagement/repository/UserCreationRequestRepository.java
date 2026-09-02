package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.UserCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
public interface UserCreationRequestRepository extends JpaRepository<UserCreationRequest,Long>{
 @EntityGraph(attributePaths={"proposedBy","proposedRole","dgmDecidedBy","gmDecidedBy"})
 List<UserCreationRequest> findByStatusOrderByCreatedAtAsc(String status);
 boolean existsByProposedUsernameAndStatusIn(String username,List<String> statuses);
 boolean existsByProposedEmployeeNumberAndStatusIn(String employeeNumber,List<String> statuses);
 boolean existsByProposedEmailIgnoreCaseAndStatusIn(String email,List<String> statuses);
}
