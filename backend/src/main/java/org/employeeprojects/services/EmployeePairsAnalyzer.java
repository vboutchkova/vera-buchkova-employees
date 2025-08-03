package org.employeeprojects.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.employeeprojects.dto.EmployeeToProject;
import org.employeeprojects.dto.MaxPairResult;
import org.employeeprojects.dto.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmployeePairsAnalyzer {

    @Autowired
    private ProjectsFileParser parser;

    public MaxPairResult analyzePairs(MultipartFile file, String dateFormat) {
        LocalDate dateForNull = LocalDate.now();

        Map<String, Map<String, List<LocalDate>>> employeesFromFile = parser.processFile(file, dateForNull, dateFormat);
        Map<Pair<String, String>, Map<String, Long>> pairsToProjects = this.findCommonProjects(employeesFromFile);

        Map.Entry<Pair<String, String>, Map<String, Long>> maxEntry =
                pairsToProjects.entrySet().stream()
                        .max(Comparator.comparingLong(e ->
                                e.getValue().values().stream().mapToLong(Long::longValue).sum()
                        ))
                        .orElse(null); // ако е празна мапа

        if (maxEntry != null) {
            Pair<String, String> maxPair = maxEntry.getKey();
            long totalDays = maxEntry.getValue().values().stream().mapToLong(Long::longValue).sum();
            return getResult(maxPair, totalDays, pairsToProjects.get(maxPair));

        } else {
            return getResult("No maximal pair found");
        }
    }

    private MaxPairResult getResult(Pair<String, String> pair, Long allTme, Map<String, Long> allCommonProjects) {
        String resutlMsg = "Maximal pair is " + pair.getFirst() + ", " + pair.getSecond() + " with total time on common projects: " + allTme;
        return new MaxPairResult(resutlMsg, pair.getFirst(), pair.getSecond(), allTme, allCommonProjects);
    }

    private MaxPairResult getResult(String message) {
        return new MaxPairResult(message, null, null, null, null);
    }

    private Map<Pair<String, String>, Map<String, Long>> findCommonProjects(Map<String, Map<String, List<LocalDate>>> employeesFromFile) {
        Map<Pair<String, String>, Map<String, Long>> pairsToProjects = new HashMap<>();

        List<String> employeesIds = employeesFromFile.keySet().stream().sorted().toList();
        for (int i = 0; i < employeesIds.size() - 1; i++) {
            for (int j = i + 1; j < employeesIds.size(); j++) {
                Pair<String, String> pair = new Pair<>();
                String emp1 = employeesIds.get(i);
                String emp2 = employeesIds.get(j);
                pair.setFirst(emp1);
                pair.setSecond(emp2);
                Map<String, Long> commonProjects = employeesFromFile.get(emp1).keySet().stream().filter(k -> employeesFromFile.get(emp2).containsKey(k))
                        .collect(Collectors.toMap(
                                id -> id,
                                id -> getCommonDays(employeesFromFile.get(emp1).get(id), employeesFromFile.get(emp2).get(id))
                        ));
                pairsToProjects.put(pair, commonProjects);
            }
        }
        return pairsToProjects;

    }

    private static long getCommonDays(List<LocalDate> period1, List<LocalDate> period2) {
        if (period2.get(0).isBefore(period1.get(0))) {
            return getCommonDays(period2, period1);
        }
        if (period1.get(1).isBefore(period2.get(0))) {
            return 0;
        }
        LocalDate end = period1.get(1).isBefore(period2.get(1)) ? period1.get(1) : period2.get(1);
        return ChronoUnit.DAYS.between(period2.get(0), end) + 1;
    }

}
