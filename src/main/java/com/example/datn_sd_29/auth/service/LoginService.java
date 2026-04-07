package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.LoginRequest;
import com.example.datn_sd_29.auth.dto.LoginResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        String normalizedInput = request.getEmail().trim().toLowerCase();
        
        // Try employee first (by username)
        Optional<Employee> employeeOpt = employeeRepository.findByUsernameIgnoreCase(normalizedInput);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            
            if (Boolean.FALSE.equals(employee.getIsActive())) {
                throw new IllegalArgumentException("Tài khoản này đã bị khóa");
            }

            String encodePassword = employee.getPassword();
            if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
                throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
            }

            // For employee: use USERNAME as JWT subject (not email)
            String token = jwtService.generateTokenWithRole(employee.getUsername(), employee.getRole(), employee.getId());
            return new LoginResponse(
                token, 
                employee.getEmail(), 
                employee.getRole(), 
                employee.getId(), 
                employee.getFullName(), 
                employee.getUsername(),
                null
            );
        }
        
        // Try customer (by email)
        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedInput)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(customer.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa");
        }

        String encodePassword = customer.getPassword();
        if (encodePassword == null || !passwordEncoder.matches(request.getPassword(), encodePassword)) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        // For customer: use EMAIL as JWT subject
        String token = jwtService.generateTokenWithRole(customer.getEmail(), "USER", customer.getId());
        return new LoginResponse(
            token, 
            customer.getEmail(), 
            "USER", 
            customer.getId(), 
            customer.getFullName(), 
            customer.getEmail(),
            customer.getPhoneNumber()
        );
    }
}
