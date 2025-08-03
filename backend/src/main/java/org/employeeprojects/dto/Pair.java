package org.employeeprojects.dto;

import lombok.Data;

@Data
public class Pair<T, S> {
    T first;
    S second;
}
