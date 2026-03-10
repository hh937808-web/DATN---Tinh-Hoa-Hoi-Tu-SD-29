package com.example.datn_sd_29.customer.service;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public List<Customer> searchCustomer(
            String phone,
            String name,
            String email,
            Boolean status
    ){
        return customerRepository.searchCustomer(phone,name,email,status);
    }

    public List<Customer> sortCustomer(String field, String direction){

        // kiểm tra field hợp lệ
        if(!List.of("id","createdAt","loyaltyPoints").contains(field)){
            throw new RuntimeException("Field sort không hợp lệ");
        }

        Sort sort;

        if(direction.equalsIgnoreCase("desc")){
            sort = Sort.by(field).descending();
        }else{
            sort = Sort.by(field).ascending();
        }

        return customerRepository.findAll(sort);
    }
}
