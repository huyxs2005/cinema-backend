package com.cinema.hub.backend.dto.auditorium;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditoriumRequest {

    @NotBlank(message = "Tên phòng chiếu bắt buộc phải nhập")
    private String name;

    @NotNull(message = "Số hàng ghế bắt buộc phải nhập")
    @Min(value = 1, message = "Số hàng ghế phải lớn hơn 0")
    private Integer numberOfRows;

    @NotNull(message = "Số ghế mỗi hàng bắt buộc phải nhập")
    @Min(value = 1, message = "Số ghế mỗi hàng phải lớn hơn 0")
    private Integer numberOfColumns;

    @NotNull(message = "Trạng thái bắt buộc phải chọn")
    private Boolean active;
}
