package tn.demo.project.service;

import tn.demo.common.domain.EmailAddress;

public interface EmailNotificationPolicy {
    boolean notificationToEmailIsAllowed(EmailAddress emailAddress);
}
