package tn.demo.project.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.demo.project.domain.ProjectId;
import tn.demo.project.view.ProjectView;
import tn.demo.project.view.ProjectViewService;
import tn.demo.project.service.ProjectService;
import tn.demo.project.view.ProjectsView;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService service;
    private final ProjectViewService projectViewService;

    public ProjectController(ProjectService service, ProjectViewService projectViewService) {
        this.service = service;
        this.projectViewService = projectViewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> create(@RequestBody ProjectInput projectInput) {
        UUID projectId = service.createProject(projectInput.name(), projectInput.description(), projectInput.estimatedEndDate(), projectInput.estimation(), projectInput.contactPersonInput()).value();
        return createdPath("/projects/"+projectId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectView> getOne(@PathVariable UUID id) {
        return projectViewService.findOne(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProjectsView>> findAll() {
        return ResponseEntity.ok(projectViewService.findAll());
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<Void> addTask(@PathVariable UUID id, @RequestBody TaskInput taskInput) {
        UUID taskId = service.addTaskTo(new ProjectId(id), taskInput.name(), taskInput.description(), taskInput.estimation()).value();
        return createdPath("/projects/"+id+"/tasks/"+taskId);
    }

    private ResponseEntity<Void> createdPath(String path){
        return ResponseEntity.created(URI.create(path)).build();
    }

}