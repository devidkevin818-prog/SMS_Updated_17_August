package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Override
    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    Optional<Employee> findById(Long id);

    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumberAndIdNot(String employeeNumber, Long id);

    @Override
    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    Page<Employee> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    List<Employee> findAll();

    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    Page<Employee> findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String employeeNumber, String fullName, Pageable pageable);

    @EntityGraph(attributePaths = {"designation", "department", "branch", "employeeStatus"})
    @Query("""
            select e from Employee e
            where (lower(e.employeeNumber) like lower(concat('%', :query, '%'))
                   or lower(e.fullName) like lower(concat('%', :query, '%')))
              and (:departmentId is null or e.department.departmentId = :departmentId)
              and (:designationId is null or e.designation.designationId = :designationId)
              and (:branchId is null or e.branch.branchId = :branchId)
            """)
    Page<Employee> filter(String query, Long departmentId, Long designationId, Long branchId, Pageable pageable);
}
