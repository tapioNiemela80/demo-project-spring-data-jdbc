package tn.demo.common;

import org.springframework.stereotype.Component;
import tn.demo.project.domain.ProjectId;
import tn.demo.project.domain.ProjectTaskId;
import tn.demo.team.domain.TeamId;
import tn.demo.team.domain.TeamMemberId;
import tn.demo.team.domain.TeamTaskId;

import java.util.UUID;

@Component
public class IDService {
    public ProjectId newProjectId(){
        return new ProjectId(newUUID());
    }

    public ProjectTaskId newProjectTaskId(){
        return new ProjectTaskId(newUUID());
    }

    public TeamId newTeamId(){
        return new TeamId(newUUID());
    }

    public TeamTaskId newTeamTaskId(){
        return new TeamTaskId(newUUID());
    }

    public TeamMemberId newTeamMemberId(){
        return new TeamMemberId(newUUID());
    }

    private UUID newUUID(){
        return UUID.randomUUID();
    }

}
