package tn.demo.project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.demo.common.EmailClientService;
import tn.demo.common.EmailMessage;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.project.domain.*;
import tn.demo.project.events.TaskAddedToProjectEvent;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.domain.TeamTaskId;
import tn.demo.team.events.TeamTaskCompletedEvent;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainEventListenersTest {
    @Mock
    private ProjectRepository projects;
    @Mock
    private EmailClientService emailClientService;

    private DomainEventListeners underTest;
    private String sender ="example.project@demo.org";

    @BeforeEach
    void setup() {
        underTest = new DomainEventListeners(projects, emailClientService, sender);
    }

    @Test
    void marksProjectTaskCompletedOnTeamTaskCompletedEvent(){
        TeamTaskId taskID = new TeamTaskId(UUID.randomUUID());
        ProjectTaskId projectTaskId = new ProjectTaskId(UUID.randomUUID());

        TeamTaskCompletedEvent teamTaskCompletedEvent = new TeamTaskCompletedEvent(taskID, projectTaskId, ActualSpentTime.fromMinutes(50));
        Project project = Mockito.mock(Project.class);
        when(projects.findByTaskId(projectTaskId.value())).thenReturn(Optional.of(project));
        when(project.completeTask(projectTaskId, ActualSpentTime.fromMinutes(50))).thenReturn(project);

        underTest.on(teamTaskCompletedEvent);
        verify(projects).save(project);
    }

    @Test
    void sendsEmailOnTaskAddedToProjectEvent(){
        Project project = Mockito.mock(Project.class);
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        ProjectTaskId taskId = new ProjectTaskId(UUID.randomUUID());
        when(projects.findById(projectId.value())).thenReturn(Optional.of(project));
        when(project.getTask(taskId)).thenReturn(Optional.of(new ProjectTaskSnapshot(taskId, projectId, "title", "desc", TimeEstimation.fromMinutes(1))));
        TaskAddedToProjectEvent taskAddedToProjectEvent = new TaskAddedToProjectEvent(projectId, taskId);

        underTest.on(taskAddedToProjectEvent);
        verify(emailClientService).send(any(EmailMessage.class));
    }
}