package com.example.datn_sd_29.employee.service;

import com.example.datn_sd_29.employee.dto.EmployeeRequest;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.enums.Gender;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // ====== GET ALL ======
    // Hiển thị cả tài khoản đã bị khóa (isActive=false) — admin cần thấy để mở lại
    // và để truy vết. Chỉ ẩn ADMIN role khỏi màn quản lý nhân viên.
    public List<Employee> getAll() {
        return employeeRepository.findAll().stream()
                .filter(e -> !"ADMIN".equalsIgnoreCase(e.getRole()))
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

        // Validate password is provided when creating new employee
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Password không được để trống khi tạo nhân viên mới");
        }

        Employee emp = new Employee();

        emp.setFullName(request.getFullName());
        emp.setUsername(request.getUsername());
        // Hash password before saving to database
        emp.setPassword(passwordEncoder.encode(request.getPassword()));
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

        // AUDIT DIFF — snapshot BEFORE modification (không capture password vì nhạy cảm)
        java.util.Map<String, Object> before = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                emp, "fullName", "username", "role", "gender", "address", "phoneNumber", "email", "isActive"
        );

        emp.setFullName(request.getFullName());
        emp.setUsername(request.getUsername());

        // Only update password if it's provided and different from current hash
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String newPassword = request.getPassword();
            if (!newPassword.startsWith("$2a$") && !newPassword.startsWith("$2b$")) {
                emp.setPassword(passwordEncoder.encode(newPassword));
            }
        }

        emp.setRole(request.getRole());
        emp.setGender(request.getGender());
        emp.setAddress(request.getAddress());
        emp.setPhoneNumber(request.getPhoneNumber());
        emp.setEmail(request.getEmail());
        emp.setIsActive(request.getIsActive());

        Employee saved = employeeRepository.save(emp);

        // AUDIT DIFF — diff trước/sau
        java.util.Map<String, Object> after = com.example.datn_sd_29.audit.util.AuditDiffUtil.snapshot(
                saved, "fullName", "username", "role", "gender", "address", "phoneNumber", "email", "isActive"
        );
        com.example.datn_sd_29.audit.context.AuditContext.setChanges(
                com.example.datn_sd_29.audit.util.AuditDiffUtil.diff(before, after, EMPLOYEE_FIELD_LABELS)
        );

        return saved;
    }

    private static final java.util.Map<String, String> EMPLOYEE_FIELD_LABELS = java.util.Map.of(
            "fullName", "Họ tên",
            "username", "Tài khoản",
            "role", "Vai trò",
            "gender", "Giới tính",
            "address", "Địa chỉ",
            "phoneNumber", "Số điện thoại",
            "email", "Email",
            "isActive", "Trạng thái hoạt động"
    );

    // ====== DELETE / KHÓA ======
    public void delete(Integer id) {
        Employee emp = getById(id);
        emp.setIsActive(false);
        employeeRepository.save(emp);
    }

    // ====== SEARCH ======
    public List<Employee> searchEmployee(
            String keyword,
            String role,
            String gender,
            String fromDate,
            String toDate
    ) {
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();
        String rl = (role == null || role.isBlank()) ? "" : role.trim();

        Gender gd = null;
        if (gender != null && !gender.isBlank()) {
            gd = Gender.valueOf(gender.trim().toUpperCase());
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

        return employeeRepository.searchEmployee(kw, rl, gd, from, to)
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
                .filter(e -> !"ADMIN".equalsIgnoreCase(e.getRole()))
                .toList();
    }
}