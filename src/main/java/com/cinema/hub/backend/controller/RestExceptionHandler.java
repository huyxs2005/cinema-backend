package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.payment.util.PaymentException;
import com.cinema.hub.backend.service.exception.SeatSelectionException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinema.hub.backend.util.TimeProvider;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), TimeProvider.now()));
    }

    @ExceptionHandler(SeatSelectionException.class)
    public ResponseEntity<ApiError> handleSeatSelection(SeatSelectionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), TimeProvider.now()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiError> handlePayment(PaymentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), TimeProvider.now()));
    }

    public record ApiError(String message, OffsetDateTime timestamp) {
    }
}
