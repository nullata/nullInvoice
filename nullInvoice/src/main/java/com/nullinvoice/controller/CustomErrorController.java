// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @GetMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", resolveErrorMessage(statusCode, message));
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        return "error";
    }

    private String resolveErrorMessage(int statusCode, Object message) {
        String customMessage = switch (statusCode) {
            case 400 ->
                "Bad request";
            case 401 ->
                "Unauthorized";
            case 403 ->
                "Access denied";
            case 404 ->
                "Page not found";
            case 405 ->
                "Method not allowed";
            case 500 ->
                "Internal server error";
            case 502 ->
                "Bad gateway";
            case 503 ->
                "Service unavailable";
            default ->
                "An error occurred";
        };

        if (message != null && !message.toString().isBlank()) {
            return message.toString();
        }
        return customMessage;
    }
}
