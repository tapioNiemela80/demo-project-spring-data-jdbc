package tn.demo.project.service;

import org.springframework.stereotype.Component;
import tn.demo.common.DateService;
import tn.demo.common.IDService;
import tn.demo.project.controller.ContactPersonInput;
import tn.demo.project.domain.ContactPerson;
import tn.demo.project.domain.Project;
import tn.demo.project.domain.TimeEstimation;

import java.time.LocalDate;

@Component
class ProjectFactory {

    private final IDService idService;
    private final DateService dateService;

    ProjectFactory(IDService idService, DateService dateService) {
        this.idService = idService;
        this.dateService = dateService;
    }

    Project createNew(String name, String description, LocalDate estimatedEndDate, tn.demo.project.controller.TimeEstimation estimation, ContactPersonInput contactPerson){
        return Project.createNew(idService.newProjectId(), name, description, dateService.now(), estimatedEndDate, toDomain(estimation), toDomain(contactPerson));
    }

    private TimeEstimation toDomain(tn.demo.project.controller.TimeEstimation estimation) {
        return new TimeEstimation(estimation.hours(), estimation.minutes());
    }

    private ContactPerson toDomain(ContactPersonInput contactPersonInput) {
        return ContactPerson.create(contactPersonInput.name(), contactPersonInput.email());
    }
}
