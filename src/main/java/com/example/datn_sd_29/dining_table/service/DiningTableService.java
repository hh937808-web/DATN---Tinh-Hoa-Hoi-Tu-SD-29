package com.example.datn_sd_29.dining_table.service;

import com.example.datn_sd_29.dining_table.dto.DiningTableRequest;
import com.example.datn_sd_29.dining_table.dto.DiningTableResponse;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository diningTableRepository;


    public List<DiningTableResponse> getAll() {
        return diningTableRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DiningTableResponse getById(Integer id) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        return mapToResponse(diningTable);
    }

    public DiningTableResponse create(DiningTableRequest request) {
        DiningTable diningTable = new DiningTable();
        diningTable.setTableName(request.getTableName());
        diningTable.setSeatingCapacity(request.getSeatingCapacity());
        diningTable.setTableStatus(request.getTableStatus());
        diningTable.setArea(request.getArea());
        diningTable.setFloor(getFloorByArea(request.getArea()));
        diningTable.setCreatedAt(Instant.now());

        return mapToResponse(diningTableRepository.save(diningTable));
    }
    public DiningTableResponse update(Integer id, DiningTableRequest request) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        diningTable.setTableName(request.getTableName());
        diningTable.setSeatingCapacity(request.getSeatingCapacity());
        diningTable.setTableStatus(request.getTableStatus());
        diningTable.setArea(request.getArea());
        diningTable.setFloor(getFloorByArea(request.getArea()));

        return mapToResponse(diningTableRepository.save(diningTable));
    }

    public String delete(Integer id) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        diningTable.setTableStatus("OUT_OF_SERVICE");
        diningTableRepository.save(diningTable);

        return "Xóa mềm thành công - bàn chuyển sang trạng thái OUT_OF_SERVICE";
    }


    public List<DiningTableResponse> searchTables(
            String keyword,
            Integer seatingCapacity,
            String tableStatus,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String direction
    ) {
        Sort sort = Sort.unsorted();

        if (sortBy != null && !sortBy.isBlank()) {
            sort = "desc".equalsIgnoreCase(direction)
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
        }

        Instant fromInstant = null;
        Instant toInstant = null;

        if (fromDate != null) {
            fromInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }

        if (toDate != null) {
            toInstant = toDate.atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
        return diningTableRepository
                .searchTables(
                        keyword,
                        seatingCapacity,
                        tableStatus,
                        fromInstant,
                        toInstant,
                        sort
                )
                .stream()
                .map(this::mapToResponse)
                .toList();

    }


    private DiningTableResponse mapToResponse(DiningTable diningTable) {
        DiningTableResponse response = new DiningTableResponse();
        response.setId(diningTable.getId());
        response.setTableName(diningTable.getTableName());
        response.setSeatingCapacity(diningTable.getSeatingCapacity());
        response.setTableStatus(diningTable.getTableStatus());
        response.setArea(diningTable.getArea());
        response.setFloor(diningTable.getFloor());
        response.setCreatedAt(diningTable.getCreatedAt());
        return response;
    }
    private Integer getFloorByArea(String area) {
        if (area == null || area.isBlank()) {
            throw new RuntimeException("Khu vực không được để trống");
        }

        return switch (area.toUpperCase()) {
            case "A", "B", "C", "D" -> 1;
            case "E", "F" -> 2;
            default -> throw new RuntimeException("Khu vực không hợp lệ. Chỉ chấp nhận A, B, C, D, E, F");
        };
    }
}