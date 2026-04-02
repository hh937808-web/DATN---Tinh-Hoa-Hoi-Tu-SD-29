package com.example.datn_sd_29.employee.service;

import com.example.datn_sd_29.employee.dto.EmployeeRequest;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.enums.Gender;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // ====== GET ALL ======
    public List<Employee> getAll() {
        return employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsActive())
                        && !"ADMIN".equalsIgnoreCase(e.getRole()))
                .toList();
    }

    // ====== GET BY ID ======
    public Employee getById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    // ====== CREATE ======
    public Employee create(EmployeeRequest request) {

        if (employeeRepository.findByUsernameIgnoreCase(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username đã tồn tại");
        }

        Employee emp = new Employee();

        emp.setFullName(request.getFullName());
        emp.setUsername(request.getUsername());
        emp.setPassword(request.getPassword());
        emp.setRole(request.getRole());
        emp.setGender(request.getGender());
        emp.setAddress(request.getAddress());
        emp.setPhoneNumber(request.getPhoneNumber());
        emp.setEmail(request.getEmail());

        emp.setIsActive(true);
        emp.setCreatedAt(Instant.now());

        return employeeRepository.save(emp);
    }

    // ====== UPDATE ======
    public Employee update(Integer id, EmployeeRequest request) {

        Employee emp = getById(id);

        emp.setFullName(request.getFullName());
        emp.setUsername(request.getUsername());
        emp.setRole(request.getRole());
        emp.setGender(request.getGender());
        emp.setAddress(request.getAddress());
        emp.setPhoneNumber(request.getPhoneNumber());
        emp.setEmail(request.getEmail());
        emp.setIsActive(request.getIsActive());

        return employeeRepository.save(emp);
    }

    public void toggleStatus(Integer id) {
        Employee emp = getById(id);
        emp.setIsActive(!emp.getIsActive()); // 👈 ĐẢO TRUE ↔ FALSE
        employeeRepository.save(emp);
    }

    public List<Employee> searchEmployee(
            String keyword,
            String role,
            String gender,
            String fromDate,
            String toDate,
            String status   // 👈 THÊM
    ) {
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();
        String rl = (role == null || role.isBlank()) ? "" : role.trim();

        Gender gd = null;
        if (gender != null && !gender.isBlank()) {
            gd = Gender.valueOf(gender.trim().toUpperCase());
        }

        // ====== convert status ======
        Boolean st = null;
        if (status != null && !status.isBlank()) {
            st = Boolean.parseBoolean(status); // "true" / "false"
        }

        Instant from = null;
        Instant to = null;

        ZoneId zone = ZoneId.systemDefault();

        if (fromDate != null && !fromDate.isBlank()) {
            from = LocalDate.parse(fromDate)
                    .atStartOfDay(zone)
                    .toInstant();
        }

        if (toDate != null && !toDate.isBlank()) {
            to = LocalDate.parse(toDate)
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .minusNanos(1)
                    .toInstant();
        }

        return employeeRepository.searchEmployee(kw, rl, gd, from, to, st)
                .stream()
                .filter(e -> !"ADMIN".equalsIgnoreCase(e.getRole()))
                .toList();
    }

    // ====== SORT ======
    public List<Employee> sortEmployee(String sortBy, String direction) {

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        if (!sortBy.equalsIgnoreCase("id") && !sortBy.equalsIgnoreCase("createdAt")) {
            throw new RuntimeException("Chỉ được sort theo id hoặc createdAt");
        }

        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(dir, sortBy);

        return employeeRepository.findAll(sort).stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsActive())
                        && !"ADMIN".equalsIgnoreCase(e.getRole()))
                .toList();
    }
}