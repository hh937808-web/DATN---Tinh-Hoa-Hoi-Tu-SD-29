package com.example.datn_sd_29.employee.controller;

import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    // SEARCH
    @GetMapping("/search")
    public List<Employee> searchEmployee(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean status
    ) {
        return employeeService.searchEmployee(username, role, phone, email, status);
    }

    // SORT
    @GetMapping("/sort")
    public List<Employee> sortEmployee(
            @RequestParam String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return employeeService.sortEmployee(sortBy, direction);
    }
}
