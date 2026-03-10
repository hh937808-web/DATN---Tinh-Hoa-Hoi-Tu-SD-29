package com.example.datn_sd_29.customer.repository;

import com.example.datn_sd_29.customer.entity.Customer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        SELECT c FROM Customer c
        WHERE
        (:phone IS NULL OR c.phoneNumber LIKE %:phone%)
        AND (:name IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%',:name,'%')))
        AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%',:email,'%')))
        AND (:status IS NULL OR c.isActive = :status)
    """)
    //Query này cho phép search(sdt,tên,email,trangthai) với feild(phoneNumber,fullname,email,isActive)
    List<Customer> searchCustomer(
           @Param("phone") String phone,
           @Param("name") String name,
           @Param("email") String email,
           @Param("status") Boolean status
    );
    // Nếu param = null -> bỏ qua filter

    List<Customer> findAll(Sort sort);
}
