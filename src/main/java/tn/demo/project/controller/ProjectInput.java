package tn.demo.project.controller;

import java.time.LocalDate;
import java.util.Set;

public record ProjectInput(String name, String description, LocalDate estimatedEndDate, TimeEstimation estimation, ContactPersonInput contactPersonInput, Set<ApproverInput> approvers) {
}
