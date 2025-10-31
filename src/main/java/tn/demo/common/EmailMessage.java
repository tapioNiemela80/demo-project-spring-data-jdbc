package tn.demo.common;

import tn.demo.common.domain.Email;

public record EmailMessage(Email from, Email to, String subject, String content, boolean isHtml) {

    @Override
    public String toString() {
        return """
            === DUMMY EMAIL ===
            From: %s
            To: %s
            Subject: %s
            Is HTML: %s
            Content:
            %s
            ====================
            """.formatted(from.value(), to.value(), subject, isHtml, content);
    }
}