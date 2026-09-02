package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.EmployeeVersion; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.List;
public interface EmployeeVersionRepository extends JpaRepository<EmployeeVersion,Long>{
 @Query("select coalesce(max(v.versionNo),0) from EmployeeVersion v where v.employee.id=:employeeId") int maxVersion(@Param("employeeId")Long employeeId);
 List<EmployeeVersion> findByEmployeeIdOrderByVersionNoDesc(Long employeeId);
}
