package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.PersonalVoucherResponse;
import com.example.datn_sd_29.voucher.repository.PersonalVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalVoucherService {

    private final PersonalVoucherRepository personalVoucherRepository;

    public List<PersonalVoucherResponse> getAll() {
        return personalVoucherRepository.findAll()
                .stream()
                .map(PersonalVoucherResponse::new)
                .toList();
    }
}
