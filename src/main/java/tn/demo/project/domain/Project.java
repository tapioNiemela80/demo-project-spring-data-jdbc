package tn.demo.project.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.common.domain.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AggregateRoot
@Table("projects")
public class Project implements Persistable<UUID> {
    @Id
    private final UUID id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDate plannedEndDate;
    private final ProjectStatus status;
    @Version
    private final int version;
    @Column("initial_estimated_time_hours")
    private final int initialEstimationHours;
    @Column("initial_estimated_time_minutes")
    private final int initialEstimationMinutes;
    @MappedCollection(idColumn = "project_id", keyColumn = "id")
    private final Set<ProjectTask> tasks;
    private final String contactPersonName;
    private final String contactPersonEmail;
    @Transient
    private final transient boolean isNew;

    public static Project createNew(ProjectId projectId, String name, String description, LocalDateTime now, LocalDate plannedEndDate, TimeEstimation timeEstimation, String contactPersonName, String contactPersonEmail) {
        return new Project(projectId.value(), name, description, now, plannedEndDate, ProjectStatus.PLANNED, 0, true, new HashSet<>(), timeEstimation, contactPersonName, contactPersonEmail);
    }

    private Project(UUID id, String name, String description,
                    LocalDateTime createdAt, LocalDate plannedEndDate, ProjectStatus status,
                    int version, boolean isNew, Set<ProjectTask> tasks,
                    TimeEstimation originalEstimatedTime, String contactPersonName, String contactPersonEmail) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.plannedEndDate = plannedEndDate;
        this.status = status;
        this.version = version;
        this.tasks = tasks;
        this.isNew = isNew;
        this.initialEstimationHours = originalEstimatedTime.getHours();
        this.initialEstimationMinutes = originalEstimatedTime.getMinutes();
        this.contactPersonName = contactPersonName;
        this.contactPersonEmail = contactPersonEmail;
    }

    @PersistenceCreator
    private Project(UUID id, String name, String description, LocalDateTime createdAt,
                    LocalDate plannedEndDate, ProjectStatus status, int version, Set<ProjectTask> tasks,
                    int initialEstimationHours, int initialEstimationMinutes,
                    String contactPersonName, String contactPersonEmail){
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.plannedEndDate = plannedEndDate;
        this.status = status;
        this.version = version;
        this.isNew = false;
        this.tasks = tasks;
        this.initialEstimationHours = initialEstimationHours;
        this.initialEstimationMinutes = initialEstimationMinutes;
        this.contactPersonName = contactPersonName;
        this.contactPersonEmail = contactPersonEmail;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Project addTask(ProjectTaskId taskId, String taskName, String description, TimeEstimation estimation){
        if(isCompleted()){
            throw new ProjectAlreadyCompletedException(new ProjectId(id));
        }
        var currentTotalEstimation = getEstimationOfAllTasks();
        var newEstimation = currentTotalEstimation.add(estimation);
        if (newEstimation.exceedsOther(getInitialEstimation())){
            throw new ProjectTimeEstimationWouldBeExceededException("Cannot add any more tasks, project estimation would be exceeded");
        }
        Set<ProjectTask> existingTasks = new HashSet<>(tasks);
        existingTasks.add(ProjectTask.createNew(taskId, taskName, description, estimation));
        return new Project(id, name, this.description, createdAt, plannedEndDate, status, version, false, existingTasks,
                getInitialEstimation(), contactPersonName, contactPersonEmail);
    }

    TimeEstimation getInitialEstimation(){
        return new TimeEstimation(initialEstimationHours, initialEstimationMinutes);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Project completeTask(ProjectTaskId projectTaskId, ActualSpentTime actualSpentTime) {
        verifyContainsTask(projectTaskId);

        var processedTasks = tasks.stream()
                .map(processTask(projectTaskId, actualSpentTime))
                .collect(Collectors.toSet());
        var newStatus = areAllTasksCompleted(processedTasks)
                ? ProjectStatus.COMPLETED
                : status;
        return new Project(id, name, this.description, createdAt, plannedEndDate, newStatus, version, false,
                processedTasks, getInitialEstimation(), contactPersonName, contactPersonEmail);
    }

    private void verifyContainsTask(ProjectTaskId projectTaskId) {
        tasks.stream()
                .filter(task -> task.hasId(projectTaskId))
                .findFirst()
                .orElseThrow(() -> new UnknownProjectTaskIdException(projectTaskId));
    }

    public boolean isCompleted(){
        return status == ProjectStatus.COMPLETED;
    }

    private static boolean areAllTasksCompleted(Set<ProjectTask> tasks){
        return tasks.stream()
                .allMatch(ProjectTask::isCompleted);
    }

    private Function<ProjectTask, ProjectTask> processTask(ProjectTaskId projectTaskId, ActualSpentTime actualSpentTime){
        return task -> {
            if(task.hasId(projectTaskId)){
                return task.complete(actualSpentTime);
            }
            return task;
        };
    }

    public Optional<ProjectTaskSnapshot> getTask(ProjectTaskId projectTaskId){
        return tasks.stream()
                .filter(task -> task.hasId(projectTaskId))
                .map(task -> task.toSnapshot(new ProjectId(this.getId())))
                .findFirst();
    }

    public TimeEstimation getEstimationOfAllTasks(){
        return tasks.stream()
                .map(ProjectTask::getEstimation)
                .reduce(TimeEstimation::add)
                .orElseGet(TimeEstimation::zeroEstimation);
    }

    public String getContactPersonEmail() {
        return contactPersonEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project other = (Project) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}