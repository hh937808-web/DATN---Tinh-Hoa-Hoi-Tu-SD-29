package com.example.datn_sd_29.employee.service;

import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployServiceImp implements EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public List<Employee> searchEmployee(String username, String role,
                                         String phone, String email, Boolean status) {
        return employeeRepository.searchEmployee(username, role, phone, email, status);
    }

    @Override
    public List<Employee> sortEmployee(String sortBy, String direction) {

        Sort sort;

        if (direction.equalsIgnoreCase("desc")) {
            sort = Sort.by(sortBy).descending();
        } else {
            sort = Sort.by(sortBy).ascending();
        }

        return employeeRepository.findAll(sort);
    }
}
