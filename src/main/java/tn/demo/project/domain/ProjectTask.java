package tn.demo.project.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import tn.demo.common.domain.ActualSpentTime;

import java.util.Objects;
import java.util.UUID;

@Table("project_tasks")
class ProjectTask {
    @Id
    private final UUID id;

    private final String title;

    private final String description;

    @Column("estimated_time_hours")
    private final int estimatedTimeHours;

    @Column("estimated_time_minutes")
    private final int estimatedTimeMinutes;

    private final TaskStatus taskStatus;

    @Column("actual_time_spent_hours")
    private final Integer actualTimeSpentHours;

    @Column("actual_time_spent_minutes")
    private final Integer actualTimeSpentMinutes;

    @PersistenceCreator
    private ProjectTask(UUID id, String title, String description, int estimatedTimeHours, int estimatedTimeMinutes, TaskStatus taskStatus, Integer actualTimeSpentHours, Integer actualTimeSpentMinutes){
        this.id = id;
        this.title = title;
        this.description = description;
        this.estimatedTimeHours = estimatedTimeHours;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.taskStatus = taskStatus;
        this.actualTimeSpentHours = actualTimeSpentHours;
        this.actualTimeSpentMinutes = actualTimeSpentMinutes;
    }

    static ProjectTask createNew(ProjectTaskId taskId, String title, String description, TimeEstimation estimation){
        return new ProjectTask(taskId.value(), title, description, estimation.getHours(), estimation.getMinutes(), TaskStatus.INCOMPLETE, null, null);
    }

    ProjectTask complete(ActualSpentTime actualSpentTime){
        return new ProjectTask(id, title, description, estimatedTimeHours, estimatedTimeMinutes, TaskStatus.COMPLETE, actualSpentTime.getHours(), actualSpentTime.getMinutes());
    }

    boolean hasId(ProjectTaskId expected){
        return expected.equals(new ProjectTaskId(id));
    }

    TimeEstimation getEstimation(){
        return new TimeEstimation(estimatedTimeHours, estimatedTimeMinutes);
    }

    ProjectTaskSnapshot toSnapshot(ProjectId projectId) {
        return new ProjectTaskSnapshot(new ProjectTaskId(id), projectId, title, description, getEstimation());
    }

    public boolean isCompleted() {
        return taskStatus == TaskStatus.COMPLETE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectTask other = (ProjectTask) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
