package com.cinema.hub.backend.dto.auditorium;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
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
    @Max(value = 26, message = "Số hàng ghế tối đa là 26 (A-Z)")
    private Integer numberOfRows;

    @NotNull(message = "Số ghế mỗi hàng bắt buộc phải nhập")
    @Min(value = 1, message = "Số ghế mỗi hàng phải lớn hơn 0")
    @Max(value = 20, message = "Số ghế mỗi hàng tối đa là 20")
    private Integer numberOfColumns;

    @NotNull(message = "Số hàng ghế thường bắt buộc phải nhập")
    @Min(value = 0, message = "Số hàng ghế thường không hợp lệ")
    @Max(value = 26, message = "Số hàng ghế thường tối đa 26")
    private Integer normalRowCount;

    @NotNull(message = "Số hàng ghế đôi bắt buộc phải nhập")
    @Min(value = 0, message = "Số hàng ghế đôi không hợp lệ")
    @Max(value = 26, message = "Số hàng ghế đôi tối đa 26")
    private Integer coupleRowCount;

    @NotNull(message = "Trạng thái bắt buộc phải chọn")
    private Boolean active;

    @AssertTrue(message = "Tổng số hàng Standard và Couple không được vượt quá tổng số hàng.")
    public boolean isValidRowAllocation() {
        if (numberOfRows == null) {
            return true;
        }
        int standard = normalize(normalRowCount);
        int couple = normalize(coupleRowCount);
        return standard + couple <= numberOfRows;
    }

    private int normalize(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
