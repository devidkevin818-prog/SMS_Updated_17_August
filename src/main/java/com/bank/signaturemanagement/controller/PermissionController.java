package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.PermissionAdminService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/permissions")
@org.springframework.security.access.prepost.PreAuthorize("@accessControl.has(authentication.name,'USER_MANAGE')")
public class PermissionController {
    private final PermissionAdminService service;
    public PermissionController(PermissionAdminService service) { this.service=service; }

    @GetMapping
    public String index(@RequestParam(required=false) Long roleId,
                        @RequestParam(required=false) Long userId, Model model) {
        var roles=service.roles();
        var users=service.users();
        long selected=roleId!=null?roleId:(roles.isEmpty()?0:roles.getFirst().id());
        long selectedUser=userId!=null?userId:(users.isEmpty()?0:users.getFirst().id());
        model.addAttribute("roles",roles); model.addAttribute("selectedRoleId",selected);
        model.addAttribute("permissions",selected==0?java.util.List.of():service.permissions(selected));
        model.addAttribute("users",users); model.addAttribute("selectedUserId",selectedUser);
        model.addAttribute("userPermissions",selectedUser==0?java.util.List.of():service.userPermissions(selectedUser));
        return "admin/permissions";
    }

    @PostMapping("/grant")
    public String grant(@RequestParam long roleId,@RequestParam long permissionId,
                        @RequestParam(defaultValue="false") boolean granted, Authentication auth,
                        RedirectAttributes redirect) {
        try { service.setGrant(roleId,permissionId,granted,auth.getName()); redirect.addFlashAttribute("success","Permission updated"); }
        catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error",exception.getMessage()); }
        return "redirect:/admin/permissions?roleId="+roleId;
    }

    @PostMapping("/users/{id}/override")
    public String userOverride(@PathVariable long id,@RequestParam long permissionId,@RequestParam String mode,
                               Authentication auth,RedirectAttributes redirect) {
        try { service.setUserOverride(id,permissionId,mode,auth.getName()); redirect.addFlashAttribute("success","User override updated"); }
        catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error",exception.getMessage()); }
        return "redirect:/admin/permissions?userId="+id;
    }

    @PostMapping("/roles/{id}/hierarchy")
    public String hierarchy(@PathVariable long id,@RequestParam int hierarchyOrder,Authentication auth,RedirectAttributes redirect) {
        try { service.updateHierarchy(id,hierarchyOrder,auth.getName()); redirect.addFlashAttribute("success","Role hierarchy updated"); }
        catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error",exception.getMessage()); }
        return "redirect:/admin/permissions?roleId="+id;
    }
}
