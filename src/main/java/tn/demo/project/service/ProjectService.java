package tn.demo.project.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.IDService;
import tn.demo.project.controller.ContactPersonInput;
import tn.demo.project.domain.*;
import tn.demo.project.events.TaskAddedToProjectEvent;
import tn.demo.project.repository.ProjectRepository;

import java.time.LocalDate;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final IDService IDService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProjectFactory projectFactory;

    public ProjectService(ProjectRepository projects, tn.demo.common.IDService IDService, ApplicationEventPublisher applicationEventPublisher, ProjectFactory projectFactory) {
        this.projects = projects;
        this.IDService = IDService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.projectFactory = projectFactory;
    }

    @Transactional
    public ProjectId createProject(String name, String description, LocalDate estimatedEndDate, tn.demo.project.controller.TimeEstimation estimation, ContactPersonInput contactPerson) {
        Project project = projectFactory.createNew(name, description, estimatedEndDate, estimation, contactPerson);
        return new ProjectId(projects.save(project).getId());
    }

    @Transactional
    public ProjectTaskId addTaskTo(ProjectId projectId, String taskName, String description, tn.demo.project.controller.TimeEstimation estimation){
        ProjectTaskId taskId = IDService.newProjectTaskId();
        return projects.findById(projectId.value())
                .map(project -> project.addTask(taskId, taskName, description, toDomain(estimation)))
                .map(projects::save)
                .map(project -> publishNewTaskAddedToProjectEvent(projectId, taskId))
                .orElseThrow(() -> new UnknownProjectIdException(projectId.value()));
    }

    private ProjectTaskId publishNewTaskAddedToProjectEvent(ProjectId projectId, ProjectTaskId taskId) {
        applicationEventPublisher.publishEvent(new TaskAddedToProjectEvent(projectId, taskId));
        return taskId;
    }

    private TimeEstimation toDomain(tn.demo.project.controller.TimeEstimation estimation) {
        return new TimeEstimation(estimation.hours(), estimation.minutes());
    }

}