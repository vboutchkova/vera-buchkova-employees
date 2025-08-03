package org.employeeprojects.services;

import lombok.extern.slf4j.Slf4j;
import org.employeeprojects.dto.EmployeeToProject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ProjectsFileParser {

    @Value("${my.property.name:yyyy-MM-dd}")
    private String defaultDateFormat = "yyyy-MM-dd";
    private String UTC = "UTC";


    public Map<String, Map<String, List<LocalDate>>> processFile(MultipartFile file, LocalDate dateForNull, String dateFormat) {
        Map<String, Map<String, List<LocalDate>>> employeesFromFile = new HashMap();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("The input file should not be empty.");
        }
        DateTimeFormatter formatter = getFormatter(dateFormat);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean checkHeaderLine = true;
            while ((line = bufferedReader.readLine()) != null) {

                if (checkHeaderLine && isHeaderRow(line)) {
                    checkHeaderLine = false;
                    continue;
                }

                EmployeeToProject parsedEmployee = parseCsvLine(line, dateForNull, formatter);
                Map<String, List<LocalDate>> projects = employeesFromFile.get(parsedEmployee.employeeId());
                if (projects == null) {
                    projects = new HashMap<>();
                    List<LocalDate> projectDates = new ArrayList<>();
                    projectDates.add(parsedEmployee.from());
                    projectDates.add(parsedEmployee.to());
                    projects.put(parsedEmployee.projectId(), projectDates);
                } else {
                    List<LocalDate> projectDates = projects.get(parsedEmployee.projectId());
                    if (projectDates == null || projectDates.isEmpty()) {
                        projectDates = new ArrayList<>();
                        projectDates.add(parsedEmployee.from());
                        projectDates.add(parsedEmployee.to());
                    }
                    projects.put(parsedEmployee.projectId(), projectDates);
                }
                employeesFromFile.put(parsedEmployee.employeeId(), projects);
            }

            return employeesFromFile;

        } catch (IOException e) {
            log.error("Failed to read the input file: {} and date for null {}", e.getMessage(), dateForNull);
            throw new IllegalArgumentException("Failed to read the input file: ", e);
        } catch (RuntimeException e) {
            log.error("Unexpected error occurred while reading from file: {} and date for null {}", e.getMessage(), dateForNull);
            throw new IllegalArgumentException("Unexpected error occurred while reading from file: ", e);
        }
    }

    private boolean isHeaderRow(String line) {
        String[] parts = Arrays.stream(line.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        return parts[0].contains("EmpID") && parts[1].contains("ProjectID") && parts[2].contains("DateFrom") && parts[3].contains("DateTo");
    }

    private DateTimeFormatter getFormatter(String dateFormat) {
        if (dateFormat == null || dateFormat.isEmpty()) {
            dateFormat = this.defaultDateFormat;
        }
        return DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneId.of(UTC));
    }

    private EmployeeToProject parseCsvLine(String csvDataLine, LocalDate dateForNull, DateTimeFormatter formatter) {
        String[] parts = Arrays.stream(csvDataLine.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        LocalDate dateFrom = LocalDate.parse(parts[2], formatter);
        LocalDate dateTo = parts[3].equalsIgnoreCase("NULL")
                ? dateForNull
                : LocalDate.parse(parts[3], formatter);

        return new EmployeeToProject(parts[0], parts[1], dateFrom, dateTo);
    }
}
