package tn.demo.projectapproval.repository;

import org.springframework.data.repository.CrudRepository;
import tn.demo.projectapproval.domain.ProjectApproval;

import java.util.UUID;

public interface ProjectApprovalRepository
        extends CrudRepository<ProjectApproval, UUID> {
}