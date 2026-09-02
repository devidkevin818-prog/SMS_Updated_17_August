package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeChangeProposal;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.EmployeeChangeProposalRepository;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeChangeProposalService {
    private static final List<String> OPEN = List.of("PD_ACTION_REQUIRED", "PD_EDITING", "PENDING_DGM", "PENDING_GM");
    private final EmployeeChangeProposalRepository proposals;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final AuditService audit;
    private final AccessControlService access;

    public EmployeeChangeProposalService(EmployeeChangeProposalRepository proposals, EmployeeRepository employees,
                                         UserRepository users, AuditService audit, AccessControlService access) {
        this.proposals = proposals; this.employees = employees; this.users = users; this.audit = audit; this.access = access;
    }

    @Transactional
    public void submit(Long employeeId, String justification, String actorName) {
        access.require(actorName, "EMPLOYEE_EDIT_PROPOSE");
        User actor = users.findByUsername(actorName).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!Set.of("DGM", "GM", "ADMIN").contains(actor.getRole().getName())) {
            throw new AccessDeniedException("Only DGM or GM may initiate a locked-record proposal");
        }
        if (justification == null || justification.isBlank()) throw new IllegalArgumentException("Justification is required");
        Employee employee = employees.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (!employee.isActive()) throw new IllegalArgumentException("Employee is inactive");
        if (proposals.existsByEmployeeIdAndStatusIn(employeeId, OPEN)) throw new IllegalArgumentException("An active proposal already exists for this employee");
        EmployeeChangeProposal proposal = new EmployeeChangeProposal(); proposal.setEmployee(employee); proposal.setRequestedBy(actor);
        proposal.setJustification(justification.trim()); proposals.saveAndFlush(proposal);
        audit.record(actorName,"EMPLOYEE_CHANGE_PROPOSE","PROPOSAL",String.valueOf(proposal.getId()),null,"SUCCESS",null,"PD_ACTION_REQUIRED",justification.trim());
    }

    @Transactional(readOnly = true)
    public List<EmployeeChangeProposal> pendingPd(String username) {
        requirePd(username);
        return proposals.findByStatusOrderByCreatedAtAsc("PD_ACTION_REQUIRED");
    }

    @Transactional
    public EmployeeChangeProposal acceptForEditing(Long id, String pd) {
        requirePd(pd);
        EmployeeChangeProposal proposal = requireStatus(id, "PD_ACTION_REQUIRED");
        proposal.setStatus("PD_EDITING"); proposal.getEmployee().setUpdateRequestStatus(true);
        audit.record(pd,"EMPLOYEE_CHANGE_PD_ACCEPT","PROPOSAL",id.toString(),null,"SUCCESS","PD_ACTION_REQUIRED","PD_EDITING",proposal.getJustification());
        return proposal;
    }

    @Transactional(readOnly = true)
    public EmployeeChangeProposal requireEditing(Long id, Long employeeId, String pd) {
        requirePd(pd);
        EmployeeChangeProposal proposal = requireStatus(id, "PD_EDITING");
        if (!proposal.getEmployee().getId().equals(employeeId)) throw new IllegalArgumentException("This proposal belongs to another employee");
        return proposal;
    }

    @Transactional
    public void markSubmitted(Long id, String pd) {
        requirePd(pd);
        EmployeeChangeProposal proposal = requireStatus(id, "PD_EDITING"); proposal.setStatus("PENDING_DGM");
        audit.record(pd,"EMPLOYEE_CHANGE_PD_SUBMIT","PROPOSAL",id.toString(),null,"SUCCESS","PD_EDITING","PENDING_DGM",null);
    }

    @Transactional
    public void markPendingGm(EmployeeChangeProposal proposal) { if (proposal != null) proposal.setStatus("PENDING_GM"); }
    @Transactional
    public void markRejected(EmployeeChangeProposal proposal) { if (proposal != null) proposal.setStatus("REJECTED"); }
    @Transactional
    public void markResubmitted(EmployeeChangeProposal proposal) { if (proposal != null) proposal.setStatus("PENDING_DGM"); }
    @Transactional
    public void markEffective(EmployeeChangeProposal proposal) { if (proposal != null) proposal.setStatus("EFFECTIVE"); }

    @Transactional
    public void toggleLock(Long employeeId, String pd) {
        requirePd(pd);
        Employee employee = employees.findById(employeeId).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        boolean old=employee.isLocked(); employee.setLocked(!old);
        audit.record(pd,employee.isLocked()?"EMPLOYEE_LOCK":"EMPLOYEE_UNLOCK","EMPLOYEE",employeeId.toString(),null,"SUCCESS",String.valueOf(old),String.valueOf(employee.isLocked()),null);
    }

    private EmployeeChangeProposal requireStatus(Long id, String expected) {
        EmployeeChangeProposal proposal=proposals.findById(id).orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        if (!expected.equals(proposal.getStatus())) throw new IllegalArgumentException("Proposal is not in the expected " + expected + " state");
        return proposal;
    }
    private void requirePd(String username) {
        User user=users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!Set.of("PD","ADMIN").contains(user.getRole().getName())) throw new AccessDeniedException("PD authority is required");
    }
}
