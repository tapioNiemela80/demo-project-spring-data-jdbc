CREATE TABLE project_demo.project_approvers (
                                   id         UUID PRIMARY KEY,
                                   project_id UUID NOT NULL REFERENCES project_demo.projects(id),
                                   name       TEXT NOT NULL,
                                   email      TEXT NOT NULL,

                                   CONSTRAINT uq_project_approvers_email
                                       UNIQUE (project_id, email)
);