package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Try to find as Employee first (ADMIN, RECEPTION, STAFF)
        Optional<Employee> employeeOpt = employeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (employeeOpt.isEmpty()) {
            // Also try username for employees
            employeeOpt = employeeRepository.findByUsernameIgnoreCase(normalizedEmail);
        }

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            
            if (Boolean.FALSE.equals(employee.getIsActive())) {
                throw new IllegalArgumentException("Tài khoản này đã bị khóa");
            }

            String encodePassword = employee.getPassword();
            if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
                throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
            }

            // Generate JWT with role for employee
            String token = jwtService.generateTokenWithRole(employee.getEmail(), employee.getRole(), employee.getId());
            return new LoginResponse(token, employee.getEmail(), employee.getRole());
        }

        // If not employee, try Customer (USER role)
        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa");
        }

        String encodePassword = customer.getPassword();
        if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        // Generate JWT with USER role for customer
        String token = jwtService.generateTokenWithRole(customer.getEmail(), "USER", customer.getId());
        return new LoginResponse(token, customer.getEmail(), "USER");
    }
}
