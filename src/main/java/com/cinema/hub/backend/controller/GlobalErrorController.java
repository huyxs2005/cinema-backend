package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.util.TimeProvider;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        int statusCode = extractStatusCode(request);
        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader != null
                && acceptHeader.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE)) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Unexpected error");
            body.put("status", statusCode);
            body.put("timestamp", TimeProvider.now());
            Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            if (path != null) {
                body.put("path", path.toString());
            }
            return ResponseEntity.status(statusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }

        return "redirect:/";
    }

    private int extractStatusCode(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusAttr == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        try {
            return Integer.parseInt(statusAttr.toString());
        } catch (NumberFormatException ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }
}
