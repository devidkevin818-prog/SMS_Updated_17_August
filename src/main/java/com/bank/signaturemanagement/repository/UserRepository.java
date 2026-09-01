package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Override
    @EntityGraph(attributePaths = "role")
    List<User> findAll();

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    @Query("""
            select u from User u
            where (:query = '' or lower(u.username) like lower(concat('%', :query, '%'))
                or lower(u.fullName) like lower(concat('%', :query, '%'))
                or lower(u.email) like lower(concat('%', :query, '%'))
                or lower(coalesce(u.employeeNumber, '')) like lower(concat('%', :query, '%')))
              and (:role = '' or u.role.name = :role)
              and (:branchId = '' or u.branchId = :branchId)
              and (:active is null or u.active = :active)
            """)
    Page<User> search(@Param("query") String query, @Param("role") String role,
                      @Param("branchId") String branchId, @Param("active") Boolean active,
                      Pageable pageable);

    @Query("""
            select u.employeeNumber from User u
            where u.employeeNumber is not null and u.employeeNumber <> ''
            group by u.employeeNumber having count(u.id) > 1
            """)
    List<String> findDuplicateEmployeeNumbers();

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumberAndIdNot(String employeeNumber, Long id);
    long countByActiveTrue();
    long countByActiveFalse();
    long countByRoleNameAndActiveTrue(String roleName);
    long countByRoleNameAndActiveTrueAndIdNot(String roleName, Long id);
}
