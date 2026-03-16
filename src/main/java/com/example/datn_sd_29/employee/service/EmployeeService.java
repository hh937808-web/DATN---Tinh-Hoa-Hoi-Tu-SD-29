package com.example.datn_sd_29.employee.service;

import com.example.datn_sd_29.employee.entity.Employee;

import java.util.List;

public interface EmployeeService {

    List<Employee> searchEmployee(String username, String role,
                                  String phone, String email, Boolean status);

    List<Employee> sortEmployee(String sortBy, String direction);
}
