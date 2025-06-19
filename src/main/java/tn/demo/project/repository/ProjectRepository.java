package tn.demo.project.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import tn.demo.project.domain.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends CrudRepository<Project, UUID> {
    @Query("""
        SELECT p.*
        FROM projects p
        JOIN project_tasks pt ON pt.project_id = p.id
        WHERE pt.id = :taskId
    """)
    Optional<Project> findByTaskId(@Param("taskId") UUID taskId);
}