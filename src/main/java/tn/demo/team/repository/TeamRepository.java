package tn.demo.team.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import tn.demo.team.domain.Team;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends CrudRepository<Team, UUID>{
    @Query("""
        SELECT t.* FROM teams t
        JOIN team_tasks tt ON t.id = tt.team_id
        WHERE tt.project_task_id = :projectTaskId
        LIMIT 1
    """)
    Optional<Team> findByOriginalProjectTaskId(@Param("projectTaskId") UUID projectTaskId);

    @Query("""
        SELECT t.* FROM teams t
        JOIN team_tasks tt ON t.id = tt.team_id
        WHERE tt.id = :taskId
        LIMIT 1
    """)
    Optional<Team> findByTaskId(@Param("taskId") UUID taskId);
}