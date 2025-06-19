package tn.demo.team.service;

import org.springframework.stereotype.Component;
import tn.demo.team.domain.Team;
import tn.demo.team.domain.TeamId;

@Component
class TeamFactory {
    Team createNew(TeamId id, String name){
        return Team.createNew(id, name);
    }
}
