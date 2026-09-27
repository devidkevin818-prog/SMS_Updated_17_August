package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.entity.ImportBatch;
import com.bank.signaturemanagement.service.BatchImportService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dgm/batch-requests")
public class BatchController {

    private final BatchImportService batchImportService;

    public BatchController(
            BatchImportService batchImportService
    ) {
        this.batchImportService = batchImportService;
    }

    /**
     * Displays one import batch and its imported rows.
     */
    @GetMapping("/{id}")
    public String viewBatchRequest(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute(
                    "batch",
                    batchImportService.getActiveBatch(id)
            );

            model.addAttribute(
                    "items",
                    batchImportService.getBatchItems(id)
            );

            return "dgm/batch-request-details";

        } catch (EntityNotFoundException
                 | IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/dgm/dashboard#batch-requests";
        }
    }

    /**
     * Handles the Level 1 Checker batch decision.
     */
    @PostMapping("/{id}/decision")
    public String decideBatchRequest(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            @RequestParam(
                    value = "comment",
                    required = false
            ) String comment,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ImportBatch updatedBatch =
                    batchImportService.makeLevel1Decision(
                            id,
                            action,
                            comment
                    );

            if ("APPROVE".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Batch "
                                + updatedBatch.getBatchNumber()
                                + " was approved and sent to "
                                + "Level 2 Checker."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Batch "
                                + updatedBatch.getBatchNumber()
                                + " was rejected."
                );
            }

        } catch (EntityNotFoundException
                 | IllegalArgumentException
                 | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/dashboard#batch-requests";
    }
}