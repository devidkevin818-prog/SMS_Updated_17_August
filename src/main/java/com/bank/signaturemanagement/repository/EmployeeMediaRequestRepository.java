package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.EmployeeMediaRequest;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface EmployeeMediaRequestRepository extends JpaRepository<EmployeeMediaRequest,Long>{@EntityGraph(attributePaths={"employee","employee.designation","employee.department","employee.branch","submittedBy"})List<EmployeeMediaRequest>findByStatusOrderBySubmittedAtAsc(String status);boolean existsByEmployeeIdAndStatusIn(Long employeeId,Collection<String> statuses);}
