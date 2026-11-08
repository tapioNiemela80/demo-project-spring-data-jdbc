package tn.demo.project.domain;

import tn.demo.common.domain.ApproverId;

public record ApproverSnapshot(
        ApproverId approverId,
        String name,
        String emailAddress
) {}