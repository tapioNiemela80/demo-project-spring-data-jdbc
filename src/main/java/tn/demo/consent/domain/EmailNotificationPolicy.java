package tn.demo.consent.domain;

import tn.demo.common.domain.EmailAddress;

public interface EmailNotificationPolicy {
    boolean notificationToEmailIsAllowed(EmailAddress emailAddress);
}
