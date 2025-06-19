package tn.demo.team.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.project.domain.ProjectTaskId;

import java.util.Objects;
import java.util.UUID;

@Table("team_tasks")
class TeamTask {
    @Id
    private final UUID id;
    private final UUID projectTaskId;
    private final String name;
    private final String description;
    private final TeamTaskStatus status;
    private final UUID assigneeId;
    @Column("actual_time_spent_hours")
    private final Integer actualTimeSpentHours;
    @Column("actual_time_spent_minutes")
    private final Integer actualTimeSpentMinutes;

    @PersistenceCreator
    private TeamTask(UUID id, UUID projectTaskId, String name, String description, TeamTaskStatus status, UUID assigneeId, Integer actualTimeSpentHours, Integer actualTimeSpentMinutes){
        this.id = id;
        this.projectTaskId = projectTaskId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.assigneeId = assigneeId;
        this.actualTimeSpentHours = actualTimeSpentHours;
        this.actualTimeSpentMinutes = actualTimeSpentMinutes;
    }

    static TeamTask createNew(TeamTaskId id, ProjectTaskId projectTaskId, String name, String description){
        return new TeamTask(id.value(), projectTaskId.value(), name, description, TeamTaskStatus.NOT_ASSIGNED, null, null, null);
    }

    boolean canBeDeleted(){
        return status == TeamTaskStatus.NOT_ASSIGNED;
    }

    TeamTask assignTo(TeamMemberId assigneeId){
        if (this.status != TeamTaskStatus.NOT_ASSIGNED) {
            throw new TaskTransitionNotAllowedException("Task already assigned or in progress.");
        }
        return new TeamTask(id, projectTaskId, name, description, TeamTaskStatus.ASSIGNED, assigneeId.value(), actualTimeSpentHours, actualTimeSpentMinutes);
    }

    TeamTask markInProgress(){
        if (this.status != TeamTaskStatus.ASSIGNED) {
            throw new TaskTransitionNotAllowedException("Task needs to be assigned before it can be put to in progress.");
        }
        return new TeamTask(id, projectTaskId, name, description, TeamTaskStatus.IN_PROGRESS, assigneeId, actualTimeSpentHours, actualTimeSpentMinutes);
    }

    TeamTask complete(ActualSpentTime actualTimeSpent) {
        if (this.status != TeamTaskStatus.IN_PROGRESS) {
            throw new TaskTransitionNotAllowedException("task not in progress");
        }
        return new TeamTask(id, projectTaskId, name, description, TeamTaskStatus.COMPLETED, null, actualTimeSpent.getHours(), actualTimeSpent.getMinutes());
    }

    TeamTask unassign() {
        if(this.status != TeamTaskStatus.ASSIGNED){
            throw new TaskTransitionNotAllowedException("Task is not assigned");
        }
        return new TeamTask(id, projectTaskId, name, description, TeamTaskStatus.NOT_ASSIGNED, null, actualTimeSpentHours, actualTimeSpentMinutes);
    }

    boolean hasId(TeamTaskId expected) {
        return id.equals(expected.value());
    }

    ProjectTaskId getOriginalTaskId() {
        return new ProjectTaskId(projectTaskId);
    }

    boolean hasDetails(TeamTaskId taskId, ProjectTaskId projectTaskId, String name, String description, TeamMemberId assignee, ActualSpentTime actualSpentTime, TeamTaskStatus expectedStatus) {
        return hasId(taskId)
                && Objects.equals(projectTaskId.value(), this.projectTaskId)
                && Objects.equals(name, this.name)
                && Objects.equals(description, this.description)
                && assigneeEquals(assignee)
                && Objects.equals(actualSpentTime, this.getActualSpentTime())
                && expectedStatus == this.status;
    }

    private boolean assigneeEquals(TeamMemberId expected){
        if(expected == null){
            return assigneeId == null;
        }
        return new TeamMemberId(assigneeId).equals(expected);
    }

    private ActualSpentTime getActualSpentTime() {
        if(status == TeamTaskStatus.COMPLETED){
            return new ActualSpentTime(actualTimeSpentHours, actualTimeSpentMinutes);
        }
        return null;
    }

    boolean isAssignedTo(TeamMemberId memberId) {
        Objects.requireNonNull(memberId);
        if(assigneeId == null){
            return false;
        }
        return assigneeId.equals(memberId.value());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamTask other = (TeamTask) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
