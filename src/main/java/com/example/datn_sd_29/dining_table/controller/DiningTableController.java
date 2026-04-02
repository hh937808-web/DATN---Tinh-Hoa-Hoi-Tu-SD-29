package com.example.datn_sd_29.dining_table.controller;

import com.example.datn_sd_29.dining_table.dto.DiningTableRequest;
import com.example.datn_sd_29.dining_table.dto.DiningTableResponse;
import com.example.datn_sd_29.dining_table.service.DiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DiningTableController {

    private final DiningTableService diningTableService;


    @GetMapping
    public List<DiningTableResponse> getAll() {
        return diningTableService.getAll();
    }


    @GetMapping("/{id}")
    public DiningTableResponse getById(@PathVariable Integer id) {
        return diningTableService.getById(id);
    }


    @PostMapping
    public DiningTableResponse create(@RequestBody DiningTableRequest request) {
        return diningTableService.create(request);
    }

    @PutMapping("/{id}")
    public DiningTableResponse update(@PathVariable Integer id,
                                      @RequestBody DiningTableRequest request) {
        return diningTableService.update(id, request);
    }


    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        return diningTableService.delete(id);
    }


    @GetMapping("/search")
    public ResponseEntity<List<DiningTableResponse>> searchTables(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer seatingCapacity,
            @RequestParam(required = false) String tableStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction
    ) {
        return ResponseEntity.ok(
                diningTableService.searchTables(
                        keyword,
                        seatingCapacity,
                        tableStatus,
                        fromDate,
                        toDate,
                        sortBy,
                        direction
                )
        );
    }
}