package com.bank.signaturemanagement.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Service
public class AuditReportService {
    private final JdbcTemplate jdbc;
    public AuditReportService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public record OnboardingRow(Long employeeId,String employeeNumber,String employeeName,LocalDateTime onboardedAt,
                                String initiatedBy,String source,String batchNumber,String dgmApprovedBy,
                                LocalDateTime dgmApprovedAt,String gmApprovedBy,LocalDateTime gmApprovedAt){}
    public record SignatureAdditionRow(Long employeeId,String employeeNumber,String employeeName,String signatureType,
                                       String addedBy,LocalDateTime addedAt,String requestNumber){}
    public record SignatureViewRow(String viewedBy,LocalDateTime viewedAt,Long employeeId,String employeeNumber,
                                   String employeeName,String signatureType,String result,String ipAddress){}
    public record TrailRow(Long id,LocalDateTime eventTime,String username,String actionType,String targetEntity,
                           String targetId,String ipAddress,String result){}

    public Page<TrailRow> trail(String query,String action,int page){String q=query==null?"":query.trim();String a=action==null?"":action.trim();int p=Math.max(0,page);int size=50;String where="""
        WHERE (?='' OR LOWER(COALESCE(username,'')) LIKE LOWER('%'+?+'%') OR LOWER(target_entity) LIKE LOWER('%'+?+'%') OR LOWER(COALESCE(target_id,'')) LIKE LOWER('%'+?+'%'))
          AND (?='' OR action_type=?)
        """;Long count=jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs "+where,Long.class,q,q,q,q,a,a);List<TrailRow> rows=jdbc.query("""
        SELECT id,event_time,username,action_type,target_entity,target_id,ip_address,result FROM audit_logs
        """+where+" ORDER BY event_time DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",(rs,n)->new TrailRow(rs.getLong(1),date(rs,2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)),q,q,q,q,a,a,p*size,size);return new PageImpl<>(rows,PageRequest.of(p,size),count==null?0:count);}

    @Transactional(readOnly=true)
    public List<OnboardingRow> onboarding(){return jdbc.query("""
        SELECT e.id,e.employee_number,e.full_name,e.created_at,
               COALESCE(uploader.username,requester.username,'Unknown/legacy') initiated_by,
               CASE WHEN b.id IS NOT NULL THEN 'BATCH' ELSE 'INDIVIDUAL' END source,
               b.batch_number,COALESCE(batch_dgm.username,request_dgm.username) dgm_user,
               COALESCE(b.dgm_decided_at,request_dgm_h.action_at) dgm_at,
               COALESCE(batch_gm.username,request_gm.username) gm_user,
               COALESCE(b.gm_decided_at,request_gm_h.action_at) gm_at
        FROM employees e
        LEFT JOIN import_batches b ON b.id=e.batch_id
        LEFT JOIN users uploader ON uploader.id=b.uploaded_by
        LEFT JOIN users batch_dgm ON batch_dgm.id=b.dgm_decided_by
        LEFT JOIN users batch_gm ON batch_gm.id=b.gm_decided_by
        OUTER APPLY (SELECT TOP(1) er.id,er.requested_by FROM employee_requests er
                     WHERE er.target_employee_id=e.id OR (er.target_employee_id IS NULL AND er.employee_code=e.employee_number AND er.status='APPROVED')
                     ORDER BY er.completed_at DESC,er.id DESC) req
        LEFT JOIN users requester ON requester.id=req.requested_by
        OUTER APPLY (SELECT TOP(1) ah.acted_by,ah.action_at FROM approval_history ah WHERE ah.request_id=req.id AND ah.approval_level='DGM' AND ah.action='APPROVED' ORDER BY ah.action_at DESC) request_dgm_h
        LEFT JOIN users request_dgm ON request_dgm.id=request_dgm_h.acted_by
        OUTER APPLY (SELECT TOP(1) ah.acted_by,ah.action_at FROM approval_history ah WHERE ah.request_id=req.id AND ah.approval_level='GM' AND ah.action='APPROVED' ORDER BY ah.action_at DESC) request_gm_h
        LEFT JOIN users request_gm ON request_gm.id=request_gm_h.acted_by
        ORDER BY e.created_at DESC,e.id DESC
        """,(rs,n)->new OnboardingRow(rs.getLong(1),rs.getString(2),rs.getString(3),date(rs,4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),date(rs,9),rs.getString(10),date(rs,11)));}

    @Transactional(readOnly=true)
    public List<SignatureAdditionRow> signatureAdditions(){return jdbc.query("""
        SELECT e.id,e.employee_number,e.full_name,v.signature_type,
               COALESCE(u.username,'Unknown/legacy') added_by,v.added_at,
               CASE WHEN v.request_id IS NULL THEN NULL ELSE CAST(v.request_id AS varchar(30)) END request_no
        FROM (
          SELECT mv.employee_id,mv.request_id,mv.approved_at added_at,'LOCAL' signature_type
          FROM employee_media_versions mv WHERE mv.signature_path IS NOT NULL
          UNION ALL
          SELECT mv.employee_id,mv.request_id,mv.approved_at,'FOREIGN'
          FROM employee_media_versions mv WHERE mv.foreign_signature_path IS NOT NULL
        ) v
        JOIN employees e ON e.id=v.employee_id
        LEFT JOIN employee_requests er ON er.id=v.request_id
        LEFT JOIN users u ON u.id=er.requested_by
        ORDER BY v.added_at DESC,e.id
        """,(rs,n)->new SignatureAdditionRow(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),date(rs,6),rs.getString(7)));}

    @Transactional(readOnly=true)
    public List<SignatureViewRow> signatureViews(){return jdbc.query("""
        SELECT a.username,a.event_time,e.id,e.employee_number,e.full_name,
               COALESCE(a.details,CASE WHEN a.target_id LIKE 'foreign-signature/%' THEN 'FOREIGN' ELSE 'LOCAL' END),
               a.result,a.ip_address
        FROM audit_logs a
        OUTER APPLY (SELECT TRY_CONVERT(bigint,
             LEFT(SUBSTRING(a.target_id,CHARINDEX('/',a.target_id)+1,1000),
                  CHARINDEX('/',SUBSTRING(a.target_id,CHARINDEX('/',a.target_id)+1,1000)+'/')-1)) employee_id) parsed
        LEFT JOIN employees e ON e.id=parsed.employee_id
        WHERE a.action_type='MEDIA_VIEW' AND a.target_entity='SIGNATURE'
        ORDER BY a.event_time DESC,a.id DESC
        """,(rs,n)->new SignatureViewRow(rs.getString(1),date(rs,2),(Long)rs.getObject(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)));}

    private static LocalDateTime date(java.sql.ResultSet rs,int column)throws java.sql.SQLException{java.sql.Timestamp value=rs.getTimestamp(column);return value==null?null:value.toLocalDateTime();}
}
