package com.example.datn_sd_29.dining_table.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiningTableRequest {

    @NotBlank(message = "Tên bàn không được để trống")
    @Size(max = 20, message = "Tên bàn tối đa 20 ký tự")
    private String tableName;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer seatingCapacity;

    @NotBlank(message = "Trạng thái không được để trống")
    private String tableStatus;

    private String area;
}