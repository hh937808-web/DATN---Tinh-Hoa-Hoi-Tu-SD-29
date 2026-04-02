package com.example.datn_sd_29.employee.controller;

import com.example.datn_sd_29.employee.dto.EmployeeRequest;
import com.example.datn_sd_29.employee.dto.EmployeeResponse;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        List<EmployeeResponse> list = employeeService.getAll()
                .stream()
                .map(EmployeeResponse::new)
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Integer id) {
        Employee emp = employeeService.getById(id);
        return ResponseEntity.ok(new EmployeeResponse(emp));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@RequestBody EmployeeRequest request) {
        Employee emp = employeeService.create(request);
        return ResponseEntity.ok(new EmployeeResponse(emp));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Integer id,
            @RequestBody EmployeeRequest request
    ) {
        Employee emp = employeeService.update(id, request);
        return ResponseEntity.ok(new EmployeeResponse(emp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        employeeService.delete(id);
        return ResponseEntity.ok("Xóa mềm thành công");
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String role,
            @RequestParam(required = false, defaultValue = "") String gender,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        return ResponseEntity.ok(
                employeeService.searchEmployee(keyword, role, gender, fromDate, toDate)
                        .stream()
                        .map(EmployeeResponse::new)
                        .toList()
        );
    }

    @GetMapping("/sort")
    public ResponseEntity<List<EmployeeResponse>> sort(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(
                employeeService.sortEmployee(sortBy, direction)
                        .stream()
                        .map(EmployeeResponse::new)
                        .toList()
        );
    }
}