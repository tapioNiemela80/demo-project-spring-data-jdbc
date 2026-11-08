CREATE TABLE project_demo.project_approvals (
                                                project_id UUID PRIMARY KEY
                                                    REFERENCES project_demo.projects(id)
                                                        ON DELETE CASCADE,
                                                started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                                version INTEGER NOT NULL
);

CREATE TABLE project_demo.project_approval_approvers (
                                                         id         UUID PRIMARY KEY,
                                                         project_id UUID NOT NULL
                                                             REFERENCES project_demo.project_approvals(project_id)
                                                                 ON DELETE CASCADE,
                                                         name       TEXT NOT NULL,
                                                         email      TEXT NOT NULL,
                                                         status     TEXT NOT NULL,
                                                         decided_at TIMESTAMP WITHOUT TIME ZONE,

                                                         CONSTRAINT uq_project_approval_approvers_email
                                                             UNIQUE (project_id, email)
);