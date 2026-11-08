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

@Service
public class ProjectApprovalService {
    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalService.class);

    private final ProjectApprovalRepository approvals;
    private final DateService dates;

    public ProjectApprovalService(
            ProjectApprovalRepository approvals,
            DateService dates) {
        this.approvals = approvals;
        this.dates = dates;
    }

    @Transactional
    public boolean startIfNotStarted(ProjectId projectId, Set<ApproverSnapshot> approvers) {
        if (approvals.existsById(projectId.value())) {
            log.info(projectId.value() + " already started");
            return false;
        }

        approvals.save(ProjectApproval.start(
                projectId,
                approvers,
                dates.now()));

        return true;
    }
}