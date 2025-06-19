package tn.demo.team.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.common.domain.AggregateRoot;
import tn.demo.project.domain.ProjectTaskId;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AggregateRoot
@Table("teams")
public class Team implements Persistable<UUID> {
    @Id
    private final UUID id;
    private final String name;
    @Version
    private final int version;
    @MappedCollection(idColumn = "team_id", keyColumn = "id")
    private final Set<TeamMember> members;

    @MappedCollection(idColumn = "team_id", keyColumn = "id")
    private final Set<TeamTask> tasks;

    @Transient
    private final transient boolean isNew;

    public static Team createNew(TeamId id, String name) {
        return new Team(id.value(), name, 0, true, new HashSet<>(), new HashSet<>());
    }

    private Team(UUID id, String name, int version, boolean isNew, Set<TeamMember> members, Set<TeamTask> tasks) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.isNew = isNew;
        this.members = members;
        this.tasks = tasks;
    }

    @PersistenceCreator
    private Team(UUID id, String name, int version, Set<TeamMember> members, Set<TeamTask> tasks){
        this.id = id;
        this.name = name;
        this.version = version;
        this.isNew = false;
        this.members = members;
        this.tasks = tasks;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Team addMember(TeamMemberId memberId, String name, String profession){
        Set<TeamMember> existingMembers = new HashSet<>(members);
        existingMembers.add(TeamMember.createNew(memberId, name, profession));
        return new Team(id, this.name,  version, false, existingMembers, this.tasks);
    }

    public boolean containsMember(TeamMemberId memberId, String name, String profession){
        return members.stream().anyMatch(member -> member.hasDetails(memberId, name, profession));
    }

    public boolean containsCompletedTask(TeamTaskId taskId, ProjectTaskId projectTaskId, String name, String description, ActualSpentTime actualSpentTime){
        return tasks.stream()
                .anyMatch(task -> task.hasDetails(taskId, projectTaskId, name, description, null, actualSpentTime, TeamTaskStatus.COMPLETED));
    }

    public boolean containsUncompletedTask(TeamTaskId taskId, ProjectTaskId projectTaskId, String name, String description, TeamMemberId assignee, TeamTaskStatus expectedStatus){
        return tasks.stream()
                .anyMatch(task -> task.hasDetails(taskId, projectTaskId, name, description, assignee, null, expectedStatus));
    }

    public Team addTask(TeamTaskId taskId, ProjectTaskId projectTaskId, String name, String description){
        Set<TeamTask> existingTasks = new HashSet<>(tasks);
        existingTasks.add(TeamTask.createNew(taskId, projectTaskId, name, description));
        return new Team(id, this.name,  version, false, this.members, existingTasks);
    }

    public Team removeTask(TeamTaskId taskId){
        verifyContainsTask(taskId);
        verifyTaskCanBeRemoved(taskId);
        var remainingTasks = tasks.stream()
                .filter(task -> !task.hasId(taskId))
                .collect(Collectors.toSet());
        return new Team(id, this.name,  version, false, this.members, remainingTasks);
    }

    public Team removeMember(TeamMemberId memberId){
        Objects.requireNonNull(memberId);
        verifyContainsMember(memberId);
        verifyMemberCanBeRemoved(memberId);
        var remainingMembers = members.stream()
                .filter(member -> !member.hasId(memberId))
                .collect(Collectors.toSet());
        return new Team(id, this.name,  version, false, remainingMembers, tasks);
    }

    private void verifyMemberCanBeRemoved(TeamMemberId memberId) {
        if(tasks.stream().anyMatch(task -> task.isAssignedTo(memberId))){
            throw new TeamMemberHasAssignedTasksException(memberId);
        }
    }

    private void verifyContainsMember(TeamMemberId memberId) {
        if(members.stream().noneMatch(member -> member.hasId(memberId))){
            throw new UnknownTeamMemberIdException(memberId);
        }
    }

    private void verifyTaskCanBeRemoved(TeamTaskId taskId) {
        var canBeDeleted = tasks.stream()
                .filter(task -> task.hasId(taskId))
                .map(TeamTask::canBeDeleted)
                .findFirst()
                .orElse(true);
        if(!canBeDeleted){
            throw new TaskCannotBeDeletedException(taskId);
        }
    }

    public Optional<ProjectTaskId> getOriginalTaskId(TeamTaskId taskId){
        return tasks.stream()
                .filter(task -> task.hasId(taskId))
                .map(TeamTask::getOriginalTaskId)
                .findFirst();
    }

    private void verifyContainsTask(TeamTaskId taskId){
        if(tasks.stream().noneMatch(task -> task.hasId(taskId))){
            throw new UnknownTeamTaskIdException(taskId);
        }
    }

    public Team assignTask(TeamTaskId taskId, TeamMemberId memberId) {
        verifyContainsTask(taskId);
        if(members.stream().noneMatch(member -> member.hasId(memberId))){
            throw new UnknownTeamMemberIdException(memberId);
        }
        var newTasks = tasks.stream()
                .map(assign(taskId, memberId))
                .collect(Collectors.toSet());
        return new Team(id, name, version, false, members, newTasks);
    }

    public Team markTaskInProgress(TeamTaskId taskId) {
        verifyContainsTask(taskId);
        var newTasks = tasks.stream()
                .map(markInProgress(taskId))
                .collect(Collectors.toSet());
        return new Team(id, name, version, false, members, newTasks);
    }

    public Team markTaskCompleted(TeamTaskId taskId, ActualSpentTime actualSpentTime) {
        verifyContainsTask(taskId);
        var newTasks = tasks.stream()
                .map(markCompleted(taskId, actualSpentTime))
                .collect(Collectors.toSet());
        return new Team(id, name, version, false, members, newTasks);
    }

    public Team markTaskUnassigned(TeamTaskId taskId) {
        verifyContainsTask(taskId);
        var newTasks = tasks.stream()
                .map(markUnassigned(taskId))
                .collect(Collectors.toSet());
        return new Team(id, name, version, false, members, newTasks);
    }
    private Function<TeamTask, TeamTask> markUnassigned(TeamTaskId taskId) {
        return teamTask -> {
            if(teamTask.hasId(taskId)){
                return teamTask.unassign();
            }
            return teamTask;
        };
    }

    private Function<TeamTask, TeamTask> markInProgress(TeamTaskId taskId) {
        return teamTask -> {
            if(teamTask.hasId(taskId)){
                return teamTask.markInProgress();
            }
            return teamTask;
        };
    }

    private Function<TeamTask, TeamTask> markCompleted(TeamTaskId taskId, ActualSpentTime actualSpentTime) {
        return teamTask -> {
            if(teamTask.hasId(taskId)){
                return teamTask.complete(actualSpentTime);
            }
            return teamTask;
        };
    }

    private Function<TeamTask, TeamTask> assign(TeamTaskId taskId, TeamMemberId memberId) {
        return teamTask -> {
            if(teamTask.hasId(taskId)){
                return teamTask.assignTo(memberId);
            }
            return teamTask;
        };
    }


    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team other = (Team) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}