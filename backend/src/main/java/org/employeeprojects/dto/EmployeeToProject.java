package org.employeeprojects.dto;

import java.time.LocalDate;

public record EmployeeToProject(
        String employeeId,
        String projectId,
        LocalDate from,
        LocalDate to) {
}
