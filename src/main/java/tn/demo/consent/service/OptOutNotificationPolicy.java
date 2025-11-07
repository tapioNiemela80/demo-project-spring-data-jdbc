package tn.demo.consent.service;

import tn.demo.common.domain.EmailAddress;
import tn.demo.consent.repository.EmailOptOutRepository;
import tn.demo.project.domain.EmailNotificationPolicy;

public class OptOutNotificationPolicy implements EmailNotificationPolicy {

    private final EmailOptOutRepository optOuts;

    public OptOutNotificationPolicy(EmailOptOutRepository optOuts) {
        this.optOuts = optOuts;
    }

    @Override
    public boolean notificationToEmailIsAllowed(EmailAddress emailAddress) {
        return !optOuts.existsByEmail(emailAddress.value());
    }
}
