package tn.demo.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.demo.projectapproval.service.ProjectApprovalOutboxProcessor;
import tn.demo.project.service.TeamTaskCompletedOutboxProcessor;
import tn.demo.project.service.ProjectApprovalResultOutboxProcessor;

@Component
public class OutboxPollingScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxHandler handler;

    public OutboxPollingScheduler(OutboxHandler handler) {
        this.handler = handler;
    }

    @Scheduled(fixedDelayString = "${outbox.polling.project-approval-result.fixed-delay-ms:5000}")
    public void pollProjectApprovalResults() {
        try {
            handler.handle(ProjectApprovalResultOutboxProcessor.LISTENER_ID);
        } catch (Exception failure) {
            log.error("Outbox polling failed for listener {}", ProjectApprovalResultOutboxProcessor.LISTENER_ID, failure);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.polling.project-complete-task.fixed-delay-ms:5000}")
    public void pollTeamTaskCompleted() {
        try {
            handler.handle(TeamTaskCompletedOutboxProcessor.LISTENER_ID);
        } catch (Exception failure) {
            log.error("Outbox polling failed for listener {}",
                    TeamTaskCompletedOutboxProcessor.LISTENER_ID, failure);
        }
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
