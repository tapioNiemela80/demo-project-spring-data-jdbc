package tn.demo.team.events;

import org.springframework.context.ApplicationEvent;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.team.domain.TeamTaskId;

import java.util.Objects;
import java.util.UUID;

public class TeamTaskCompletedEvent extends ApplicationEvent {
    private final TeamTaskId taskID;
    private final UUID teamId;
    private final ProjectTaskId projectTaskId;
    private final ActualSpentTime actualSpentTime;
    public TeamTaskCompletedEvent(UUID teamId, TeamTaskId taskID, ProjectTaskId projectTaskId, ActualSpentTime actualSpentTime) {
        super(taskID);
        this.taskID = taskID;
        this.teamId = teamId;
        this.projectTaskId = projectTaskId;
        this.actualSpentTime = actualSpentTime;
    }
    public UUID getTeamId() {
        return teamId;
    }

    public ProjectTaskId getProjectTaskId() {
        return projectTaskId;
    }

    public ActualSpentTime getActualSpentTime() {
        return actualSpentTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamTaskCompletedEvent that = (TeamTaskCompletedEvent) o;
        return Objects.equals(teamId, that.teamId) && taskID.equals(that.taskID) && projectTaskId.equals(that.projectTaskId) && actualSpentTime.equals(that.actualSpentTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, taskID, projectTaskId, actualSpentTime);
    }
}
