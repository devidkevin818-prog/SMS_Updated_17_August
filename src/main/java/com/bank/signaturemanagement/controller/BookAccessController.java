package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.repository.*;
import com.bank.signaturemanagement.service.SignatureBookService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/books")
public class BookAccessController {
    private final SignatureBookService books;
    private final UserRepository users;
    private final RoleRepository roles;

    public BookAccessController(SignatureBookService b, UserRepository u, RoleRepository r) {
        books = b;
        users = u;
        roles = r;
    }

    @GetMapping("/{id}/access")
    public String access(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("book", books.requireBook(id, auth.getName()));
        model.addAttribute("grants", books.grants(id, auth.getName()));
        model.addAttribute("users", users.findAll());
        model.addAttribute("roles", roles.findAll());
        return "admin/book-access";
    }

    @PostMapping("/{id}/access")
    public String grant(@PathVariable Long id, @RequestParam(required = false) Long userId, @RequestParam(required = false) Long roleId, @RequestParam(defaultValue = "true") boolean enabled, Authentication auth, RedirectAttributes redirect) {
        try {
            books.grant(id, userId, roleId, enabled, auth.getName());
            redirect.addFlashAttribute("success", "Book access updated");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/books/" + id + "/access";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        books.deactivate(id, auth.getName());
        redirect.addFlashAttribute("success", "Book deactivated; retained for audit");
        return "redirect:/books";
    }
}
