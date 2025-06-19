package tn.demo.team.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tn.demo.common.IDService;
import tn.demo.common.domain.ActualSpentTime;
import tn.demo.project.domain.*;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.domain.*;
import tn.demo.team.events.TeamTaskCompletedEvent;
import tn.demo.team.repository.TeamRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teams;
    @Mock
    private ProjectRepository projects;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private IDService IDService;
    @Mock
    private TeamFactory teamFactory;
    private TeamService underTest;
    @BeforeEach
    void setup() {
        underTest = new TeamService(teams, projects, applicationEventPublisher, IDService, teamFactory);
    }

    @Test
    void createsNewTeam(){
        TeamId teamId = getTeamId();
        when(IDService.newTeamId()).thenReturn(teamId);
        Team team = Mockito.mock(Team.class);
        when(teamFactory.createNew(teamId,"test team")).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        var result = underTest.createNew("test team");
        assertEquals(teamId, result);
    }
    @Test
    void addsMemberToTeam(){
        TeamId teamId = getTeamId();
        TeamMemberId memberId = getMemberId();
        when(IDService.newTeamMemberId()).thenReturn(memberId);
        Team team = Mockito.mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.addMember(memberId, "john doe", "tester")).thenReturn(team);
        when(teams.save(team)).thenReturn(team);

        var result = underTest.addMember(teamId, "john doe", "tester");
        assertEquals(memberId, result);
    }

    @Test
    void unknownTeamIdWhenTryingToAddMember(){
        TeamId teamId = getTeamId();
        TeamMemberId memberId = getMemberId();
        when(IDService.newTeamMemberId()).thenReturn(memberId);
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.addMember(teamId, "john doe", "tester"));
        verifyNoMoreInteractions(teams);
    }

    @Test
    void removesMember(){
        TeamId teamId = getTeamId();
        TeamMemberId memberId = getMemberId();
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.removeMember(memberId)).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        underTest.removeMember(teamId, memberId);
    }

    @Test
    void throwsExceptionWhenRemovingMemberOfUnknownTeam(){
        TeamId teamId = getTeamId();
        TeamMemberId memberId = getMemberId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.removeMember(teamId, memberId));
    }

    @Test
    void addsTaskToTeam(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        ProjectTaskId projectTaskId = getProjectTaskId();
        when(teams.findByOriginalProjectTaskId(projectTaskId.value())).thenReturn(Optional.empty());
        Project project = mock(Project.class);
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        ProjectTaskSnapshot projectTask = new ProjectTaskSnapshot(projectTaskId, projectId, "do test", "robot framework", TimeEstimation.fromMinutes(10));
        when(project.getTask(projectTaskId)).thenReturn(Optional.of(projectTask));
        when(projects.findByTaskId(projectTaskId.value())).thenReturn(Optional.of(project));
        when(IDService.newTeamTaskId()).thenReturn(taskId);
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.addTask(taskId, projectTask.projectTaskId(), projectTask.title(), projectTask.description())).thenReturn(team);

        var result = underTest.addTask(teamId, projectTaskId);
        assertEquals(result, taskId);
        verify(teams).save(team);
    }

    @Test
    void cannotAddTaskToTeamAsItBelongsToSomeTeamAlready(){
        TeamId teamId = getTeamId();
        ProjectTaskId projectTaskId = getProjectTaskId();
        Team team = mock(Team.class);
        when(teams.findByOriginalProjectTaskId(projectTaskId.value())).thenReturn(Optional.of(team));
        assertThrows(TaskAlreadyAssignedException.class, () -> underTest.addTask(teamId, projectTaskId));
        verifyNoMoreInteractions(teams);
    }

    @Test
    void cannotAddTaskToTeamAsUnknownTeamIdGiven(){
        TeamId teamId = getTeamId();
        ProjectTaskId projectTaskId = getProjectTaskId();
        when(teams.findByOriginalProjectTaskId(projectTaskId.value())).thenReturn(Optional.empty());
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());
        Project project = mock(Project.class);
        when(projects.findByTaskId(projectTaskId.value())).thenReturn(Optional.of(project));
        ProjectId projectId = new ProjectId(UUID.randomUUID());
        ProjectTaskSnapshot projectTask = new ProjectTaskSnapshot(projectTaskId, projectId, "do test", "robot framework", TimeEstimation.fromMinutes(10));
        when(project.getTask(projectTaskId)).thenReturn(Optional.of(projectTask));

        assertThrows(UnknownTeamIdException.class, () -> underTest.addTask(teamId, projectTaskId));
        verifyNoMoreInteractions(teams);
    }

    @Test
    void cannotAddTaskToTeamAsUnknownProjectTaskIdGiven(){
        TeamId teamId = getTeamId();
        ProjectTaskId projectTaskId = getProjectTaskId();
        when(teams.findByOriginalProjectTaskId(projectTaskId.value())).thenReturn(Optional.empty());
        Project project = mock(Project.class);
        when(project.getTask(projectTaskId)).thenReturn(Optional.empty());
        when(projects.findByTaskId(projectTaskId.value())).thenReturn(Optional.of(project));

        assertThrows(UnknownProjectTaskIdException.class, ()-> underTest.addTask(teamId, projectTaskId));
        verifyNoMoreInteractions(teams);
    }

    @Test
    void assignsTask(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        TeamMemberId memberId = getMemberId();
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.assignTask(taskId, memberId)).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        underTest.assignTask(teamId, taskId, memberId);
    }

    @Test
    void throwsExceptionWhenAssigningTaskToUnknownTeam(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        TeamMemberId memberId = getMemberId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.assignTask(teamId, taskId, memberId));
    }

    @Test
    void marksTaskInProgress(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.markTaskInProgress(taskId)).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        underTest.markTaskInProgress(teamId, taskId);
    }

    @Test
    void throwsExceptionWhenMarkingTaskInProgressOfUnknownTeam(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.markTaskInProgress(teamId, taskId));
    }

    @Test
    void unassignsTask(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.markTaskUnassigned(taskId)).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        underTest.unassignTask(teamId, taskId);
    }

    @Test
    void throwsExceptionWhenUnassigningTaskOfUnknownTeam(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.unassignTask(teamId, taskId));
    }

    @Test
    void removesTask(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        Team team = mock(Team.class);
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.removeTask(taskId)).thenReturn(team);
        when(teams.save(team)).thenReturn(team);
        underTest.removeTask(teamId, taskId);
    }

    @Test
    void throwsExceptionWhenRemovingTaskOfUnknownTeam(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());

        assertThrows(UnknownTeamIdException.class, () -> underTest.removeTask(teamId, taskId));
    }

    @Test
    void completesTask(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        Team team = mock(Team.class);
        ProjectTaskId projectTaskId = getProjectTaskId();
        when(teams.findById(teamId.value())).thenReturn(Optional.of(team));
        when(team.markTaskCompleted(taskId, new ActualSpentTime(10,0))).thenReturn(team);
        when(team.getOriginalTaskId(taskId)).thenReturn(Optional.of(projectTaskId));
        when(teams.save(team)).thenReturn(team);
        underTest.completeTask(teamId, taskId, new tn.demo.team.controller.ActualSpentTime(10, 0));

        verify(applicationEventPublisher).publishEvent(new TeamTaskCompletedEvent(taskId, projectTaskId, new ActualSpentTime(10, 0)));
    }

    @Test
    void throwsExceptionOnCompleteBecauseUnknownTeamIdGiven(){
        TeamId teamId = getTeamId();
        TeamTaskId taskId = getTaskId();
        when(teams.findById(teamId.value())).thenReturn(Optional.empty());
        assertThrows(UnknownTeamIdException.class, () -> underTest.completeTask(teamId, taskId, new tn.demo.team.controller.ActualSpentTime(10, 0)));

        verifyNoInteractions(applicationEventPublisher);
        verifyNoMoreInteractions(teams);
    }

    private ProjectTaskId getProjectTaskId() {
        return new ProjectTaskId(UUID.randomUUID());
    }

    private TeamTaskId getTaskId() {
        return new TeamTaskId(UUID.randomUUID());
    }


    private TeamMemberId getMemberId() {
        return new TeamMemberId(UUID.randomUUID());
    }

    private TeamId getTeamId(){
        return new TeamId(UUID.randomUUID());
    }

}