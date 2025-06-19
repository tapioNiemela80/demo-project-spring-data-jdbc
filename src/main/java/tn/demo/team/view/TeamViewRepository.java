package tn.demo.team.view;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

interface TeamViewRepository extends Repository<TeamViewRow, UUID> {

    @Query("""
        SELECT
            t.id AS team_id,
            t.name AS team_name,
            m.id AS member_id,
            m.name AS member_name,
            m.profession AS member_profession,
            tt.id AS task_id,
            tt.name AS task_name,
            tt.description AS task_description,
            tt.project_task_id AS project_task_id,
            tt.status AS task_status,
            tt.assignee_id AS task_assignee_id,
            tt.actual_time_spent_hours AS actual_time_spent_hours,
            tt.actual_time_spent_minutes AS actual_time_spent_minutes
        FROM project_demo.teams t
        LEFT JOIN project_demo.team_members m ON t.id = m.team_id
        LEFT JOIN project_demo.team_tasks tt ON t.id = tt.team_id
        WHERE t.id = :teamId
    """)
    List<TeamViewRow> findTeamViewByTeamId(UUID teamId);

    @Query("""
        SELECT
            t.id AS team_id,
            t.name AS team_name
        FROM project_demo.teams t
    """)
    List<TeamsViewRow> findTeams();
}