package tn.demo.team.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.demo.common.IDService;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.project.domain.ProjectTaskSnapshot;
import tn.demo.project.domain.UnknownProjectTaskIdException;
import tn.demo.project.repository.ProjectRepository;
import tn.demo.team.controller.ActualSpentTime;
import tn.demo.team.domain.*;
import tn.demo.team.events.TeamTaskCompletedEvent;
import tn.demo.team.repository.TeamRepository;

@Service
public class TeamService {
    private final TeamRepository teams;
    private final ProjectRepository projects;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final IDService IDService;
    private final TeamFactory teamFactory;

    public TeamService(TeamRepository teams, ProjectRepository projects, ApplicationEventPublisher applicationEventPublisher, tn.demo.common.IDService IDService, TeamFactory teamFactory) {
        this.teams = teams;
        this.projects = projects;
        this.applicationEventPublisher = applicationEventPublisher;
        this.IDService = IDService;
        this.teamFactory = teamFactory;
    }

    @Transactional
    public TeamId createNew(String name){
        TeamId teamId = IDService.newTeamId();
        teams.save(teamFactory.createNew(teamId, name));
        return teamId;
    }

    @Transactional
    public TeamMemberId addMember(TeamId teamId, String name, String profession){
        TeamMemberId memberId = IDService.newTeamMemberId();
        return teams.findById(teamId.value())
                .map(team -> team.addMember(memberId, name, profession))
                .map(teams::save)
                .map(ignored -> memberId)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public void removeMember(TeamId teamId, TeamMemberId memberId){
        teams.findById(teamId.value())
                .map(team -> team.removeMember(memberId))
                .map(teams::save)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public TeamTaskId addTask(TeamId teamId, ProjectTaskId projectTaskId){
        boolean alreadyBelongsToSomeTeam = checkIfAlreadyBelongsToSomeTeam(projectTaskId);
        if(alreadyBelongsToSomeTeam){
            throw new TaskAlreadyAssignedException("Task is already assigned to some team");
        }
        ProjectTaskSnapshot projectTaskSnapshot = projects.findByTaskId(projectTaskId.value())
                .flatMap(project -> project.getTask(projectTaskId))
                .orElseThrow(() -> new UnknownProjectTaskIdException(projectTaskId));
        Team team = teams.findById(teamId.value()).orElseThrow(() -> new UnknownTeamIdException(teamId));
        TeamTaskId teamTaskId = IDService.newTeamTaskId();
        Team teamWithAddedTask = team.addTask(teamTaskId, projectTaskSnapshot.projectTaskId(), projectTaskSnapshot.title(), projectTaskSnapshot.description());
        teams.save(teamWithAddedTask);
        return teamTaskId;
    }

    private boolean checkIfAlreadyBelongsToSomeTeam(ProjectTaskId originalTaskId) {
        return teams.findByOriginalProjectTaskId(originalTaskId.value()).isPresent();
    }
    @Transactional
    public void assignTask(TeamId teamId, TeamTaskId taskID, TeamMemberId toMemberId){
        teams.findById(teamId.value())
                .map(team -> team.assignTask(taskID, toMemberId))
                .map(teams::save)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public void markTaskInProgress(TeamId teamId, TeamTaskId taskID){
        teams.findById(teamId.value())
                .map(team -> team.markTaskInProgress(taskID))
                .map(teams::save)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public void unassignTask(TeamId teamId, TeamTaskId taskID){
        teams.findById(teamId.value())
                .map(team -> team.markTaskUnassigned(taskID))
                .map(teams::save)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public void removeTask(TeamId teamId, TeamTaskId taskID){
        teams.findById(teamId.value())
                .map(team -> team.removeTask(taskID))
                .map(teams::save)
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    @Transactional
    public void completeTask(TeamId teamId, TeamTaskId taskID, ActualSpentTime actualSpentTime){
        var timeSpent = toDomain(actualSpentTime);
        teams.findById(teamId.value())
                .map(team -> team.markTaskCompleted(taskID, timeSpent))
                .map(teams::save)
                .map(team -> publishTaskCompletedEvent(taskID, team, timeSpent))
                .orElseThrow(() -> new UnknownTeamIdException(teamId));
    }

    private tn.demo.common.domain.ActualSpentTime toDomain(ActualSpentTime actualSpentTime){
        return new tn.demo.common.domain.ActualSpentTime(actualSpentTime.hours(), actualSpentTime.minutes());
    }

    private Team publishTaskCompletedEvent(TeamTaskId taskID, Team team, tn.demo.common.domain.ActualSpentTime actualSpentTime) {
        team.getOriginalTaskId(taskID)
                .ifPresent(projectTaskId -> applicationEventPublisher.publishEvent(new TeamTaskCompletedEvent(taskID, projectTaskId, actualSpentTime)));
        return team;
    }

}
