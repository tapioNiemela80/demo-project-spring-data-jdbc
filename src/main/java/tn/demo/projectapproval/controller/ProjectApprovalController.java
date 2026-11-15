package tn.demo.projectapproval.controller;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.demo.project.domain.ProjectId;
import tn.demo.projectapproval.domain.ApprovalDecision;
import tn.demo.projectapproval.service.ProjectApprovalService;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/approvals")
public class ProjectApprovalController {
    private final ProjectApprovalService service;

    public ProjectApprovalController(ProjectApprovalService service) { this.service = service; }

    @PostMapping(value = "/{approverId}", consumes = "text/plain")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(@PathVariable UUID projectId, @PathVariable UUID approverId, @RequestBody String body) {
        final ApprovalDecision decision;
        try {
            decision = ApprovalDecision.valueOf(body.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected APPROVE or REJECT", ex);
        }
        try {
            service.decide(new ProjectId(projectId), approverId, decision);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalStateException | OptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }
}
