package com.example.datn_sd_29.dining_table.service;

import com.example.datn_sd_29.dining_table.dto.DiningTableRequest;
import com.example.datn_sd_29.dining_table.dto.DiningTableResponse;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository diningTableRepository;

    // GET ALL
    public List<DiningTableResponse> getAll() {
        return diningTableRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET BY ID
    public DiningTableResponse getById(Integer id) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        return mapToResponse(diningTable);
    }

    // CREATE
    public DiningTableResponse create(DiningTableRequest request) {
        DiningTable diningTable = new DiningTable();
        diningTable.setTableName(request.getTableName());
        diningTable.setSeatingCapacity(request.getSeatingCapacity());
        diningTable.setTableStatus(request.getTableStatus());

        return mapToResponse(diningTableRepository.save(diningTable));
    }

    // UPDATE
    public DiningTableResponse update(Integer id, DiningTableRequest request) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        diningTable.setTableName(request.getTableName());
        diningTable.setSeatingCapacity(request.getSeatingCapacity());
        diningTable.setTableStatus(request.getTableStatus());

        return mapToResponse(diningTableRepository.save(diningTable));
    }

    // DELETE MỀM
    public String delete(Integer id) {
        DiningTable diningTable = diningTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn có id: " + id));

        diningTable.setTableStatus("CLEANING");
        diningTableRepository.save(diningTable);

        return "Xóa mềm thành công - bàn chuyển sang trạng thái CLEANING";
    }

    private DiningTableResponse mapToResponse(DiningTable diningTable) {
        DiningTableResponse response = new DiningTableResponse();
        response.setId(diningTable.getId());
        response.setTableName(diningTable.getTableName());
        response.setSeatingCapacity(diningTable.getSeatingCapacity());
        response.setTableStatus(diningTable.getTableStatus());
        response.setCreatedAt(diningTable.getCreatedAt());
        return response;
    }




    public List<DiningTableResponse> searchByName(String tableName) {
        return diningTableRepository
                .findByTableNameContainingIgnoreCaseAndTableStatusNot(tableName, "OUT_OF_SERVICE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    public List<DiningTableResponse> searchByCapacity(Integer seatingCapacity) {
        return diningTableRepository
                .findBySeatingCapacityAndTableStatusNot(seatingCapacity, "OUT_OF_SERVICE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DiningTableResponse> searchByStatus(String tableStatus) {
        return diningTableRepository
                .findByTableStatusAndTableStatusNot(tableStatus, "OUT_OF_SERVICE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    public List<DiningTableResponse> sortBySeatingCapacity(String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by("seatingCapacity").descending()
                : Sort.by("seatingCapacity").ascending();

        return diningTableRepository
                .findByTableStatusNot("OUT_OF_SERVICE", sort)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}