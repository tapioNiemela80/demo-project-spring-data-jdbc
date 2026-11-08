package tn.demo.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.demo.projectapproval.service.ProjectApprovalOutboxProcessor;

@Component
public class OutboxPollingScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxHandler handler;

    public OutboxPollingScheduler(OutboxHandler handler) {
        this.handler = handler;
    }

    @Scheduled(
            fixedDelayString =
                    "${outbox.polling.project-start-approval-process.fixed-delay-ms:5000}"
    )
    public void pollProjectApprovalStartProcess() {
        try {
            handler.handle(ProjectApprovalOutboxProcessor.LISTENER_ID);
        } catch (Exception failure) {
            log.error(
                    "Outbox polling failed for listener {}",
                    ProjectApprovalOutboxProcessor.LISTENER_ID,
                    failure);
        }
    }
}