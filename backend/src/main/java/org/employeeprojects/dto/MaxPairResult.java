package org.employeeprojects.dto;

import java.util.List;
import java.util.Map;

public record MaxPairResult(String maxPairResultMessage, String employee1, String employee2,
                            Long time, Map<String, Long> allCommonProjects) {
}
