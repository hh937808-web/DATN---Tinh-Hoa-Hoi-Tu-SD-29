package com.example.datn_sd_29.dining_table.controller;

import com.example.datn_sd_29.dining_table.dto.DiningTableRequest;
import com.example.datn_sd_29.dining_table.dto.DiningTableResponse;
import com.example.datn_sd_29.dining_table.service.DiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dining-table")
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


    @GetMapping("/search/name")
    public List<DiningTableResponse> searchByName(@RequestParam String tableName) {
        return diningTableService.searchByName(tableName);
    }


    @GetMapping("/search/capacity")
    public List<DiningTableResponse> searchByCapacity(@RequestParam Integer seatingCapacity) {
        return diningTableService.searchByCapacity(seatingCapacity);
    }


    @GetMapping("/search/status")
    public List<DiningTableResponse> searchByStatus(@RequestParam String tableStatus) {
        return diningTableService.searchByStatus(tableStatus);
    }


    @GetMapping("/sort")
    public List<DiningTableResponse> sortBySeatingCapacity(
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return diningTableService.sortBySeatingCapacity(direction);
    }
}