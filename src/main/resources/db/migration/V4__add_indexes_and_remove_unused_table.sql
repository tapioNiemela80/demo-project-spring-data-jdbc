CREATE INDEX idx_project_tasks_project_id
    ON project_demo.project_tasks (project_id);

CREATE INDEX idx_team_tasks_project_task_id
    ON project_demo.team_tasks (project_task_id);

CREATE INDEX idx_team_tasks_team_id
    ON project_demo.team_tasks (team_id);

CREATE INDEX idx_team_members_team_id
    ON project_demo.team_members (team_id);

DROP TABLE project_demo.contact_persons;