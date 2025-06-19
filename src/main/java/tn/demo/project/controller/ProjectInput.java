package tn.demo.project.controller;

import java.time.LocalDate;

public record ProjectInput(String name, String description, LocalDate estimatedEndDate, TimeEstimation estimation, ContactPersonInput contactPersonInput) {
}
