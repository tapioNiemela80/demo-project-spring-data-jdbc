package tn.demo.project.view;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;
interface ProjectViewRepository extends Repository<ProjectTaskRow, UUID> {
    @Query("""
                SELECT
                p.id AS id,
                p.name AS name,
                p.description AS description,
                p.initial_estimated_time_hours as project_estimate_hours,
                p.initial_estimated_time_minutes as project_estimate_minutes,
                p.status as project_status,
                cp.email AS contact_person_email,
                pt.id AS task_id,
                pt.title AS task_title,
                pt.description AS task_description,
                pt.task_status as task_status,
                pt.estimated_time_hours as task_estimate_hours,
                pt.estimated_time_minutes as task_estimate_minutes,
                pt.actual_time_spent_hours as actual_hours,
                pt.actual_time_spent_minutes as actual_minutes
                FROM project_demo.projects p
                LEFT JOIN project_demo.contact_persons cp ON p.id = cp.project_id
                LEFT JOIN project_demo.project_tasks pt ON p.id = pt.project_id
                WHERE p.id = :projectId
            """)
    List<ProjectTaskRow> findProjectWithTasks(UUID projectId);

    @Query("""
            SELECT p.id,
            p.name,
            p.description
            FROM project_demo.projects p
            """)
    List<ProjectsViewRow> findAll();
}