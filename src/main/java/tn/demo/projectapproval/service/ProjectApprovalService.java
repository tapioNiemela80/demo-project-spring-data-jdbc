package tn.demo.projectapproval.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.DateService;
import tn.demo.project.domain.ApproverSnapshot;
import tn.demo.project.domain.ProjectId;
import tn.demo.projectapproval.domain.ProjectApproval;
import tn.demo.projectapproval.repository.ProjectApprovalRepository;

import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import tn.demo.projectapproval.domain.ApprovalDecision;
import tn.demo.projectapproval.events.*;

@Service
public class ProjectApprovalService {
    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalService.class);

    private final ProjectApprovalRepository approvals;
    private final DateService dates;
    private final ApplicationEventPublisher events;

    public ProjectApprovalService(
            ProjectApprovalRepository approvals,
            DateService dates, ApplicationEventPublisher events) {
        this.approvals = approvals;
        this.dates = dates;
        this.events = events;
    }

    @Transactional
    public boolean startIfNotStarted(ProjectId projectId, Set<ApproverSnapshot> approvers) {
        if (approvals.existsById(projectId.value())) {
            log.info(projectId.value() + " already started");
            return false;
        }

        var approval = ProjectApproval.start(projectId, approvers, dates.now());
        approvals.save(approval);
        events.publishEvent(new ProjectApprovalStartedEvent(projectId, approvers));
        publishOutcome(approval);

        return true;
    }
    @Transactional
    public void decide(ProjectId projectId, UUID approverId, ApprovalDecision decision) {
        var approval = approvals.findById(projectId.value())
                .orElseThrow(() -> new java.util.NoSuchElementException("Unknown approval process: " + projectId));
        var updated = approval.decide(approverId, decision, dates.now());
        if (updated == approval) { return; }
        approvals.save(updated);
        publishOutcome(updated);
    }

    private void publishOutcome(ProjectApproval approval) {
        var projectId = new ProjectId(approval.getId());
        if (approval.isApproved()) {
            events.publishEvent(new ProjectApprovedEvent(projectId));
        } else if (approval.isRejected()) {
            events.publishEvent(new ProjectRejectedEvent(projectId));
        }
    }
}