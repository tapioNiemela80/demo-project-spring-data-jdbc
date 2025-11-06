package tn.demo.project.service;

import tn.demo.common.domain.Email;

public interface EmailNotificationPolicy {
    boolean notificationToEmailIsAllowed(Email email);
}
