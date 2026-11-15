package tn.demo.team.events;

import java.util.UUID;

/** Version 1 of the durable task completion payload. */
public record TeamTaskCompletedPayload(UUID projectTaskId, int hours, int minutes) {
    public static TeamTaskCompletedPayload from(TeamTaskCompletedEvent event) {
        return new TeamTaskCompletedPayload(event.getProjectTaskId().value(),
                event.getActualSpentTime().getHours(), event.getActualSpentTime().getMinutes());
    }
}
