package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
    long countByUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("select count(e) from Employee e where e.signaturePath is not null or e.foreignSignaturePath is not null")
    long countWithSignature();

    @Query("select count(e) from Employee e where e.signaturePath is null and e.foreignSignaturePath is null")
    long countMissingSignatures();

    @Query("select count(e) from Employee e where (e.signaturePath is not null or e.foreignSignaturePath is not null) and e.signatureValidUntil is not null and e.signatureValidUntil < :today")
    long countExpiredSignatures(LocalDate today);

    @Query("select count(e) from Employee e where (e.signaturePath is not null or e.foreignSignaturePath is not null) and (e.signatureValidUntil is null or e.signatureValidUntil >= :today)")
    long countValidSignatures(LocalDate today);

    @Query("select e.department.departmentName, count(e) from Employee e group by e.department.departmentName order by count(e) desc")
    List<Object[]> countByDepartment();
}
