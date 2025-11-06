package tn.demo.consent.service;

import tn.demo.common.domain.Email;
import tn.demo.consent.repository.EmailOptOutRepository;
import tn.demo.project.service.EmailNotificationPolicy;

public class GdprPolicy implements EmailNotificationPolicy {

    private final EmailOptOutRepository optOuts;

    public GdprPolicy(EmailOptOutRepository optOuts) {
        this.optOuts = optOuts;
    }

    @Override
    public boolean notificationToEmailIsAllowed(Email email) {
        return !optOuts.existsByEmail(email.value());
    }
}
