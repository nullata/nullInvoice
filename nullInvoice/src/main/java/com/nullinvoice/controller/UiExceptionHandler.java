// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.error.ClientNotFoundException;
import com.nullinvoice.error.ConfigurationException;
import com.nullinvoice.error.InvoiceNotFoundException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackageClasses = {UiController.class, TemplateUiController.class})
@Slf4j
public class UiExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleInvoiceNotFound(InvoiceNotFoundException ex, Model model) {
        return renderError(model, 404, ex.getMessage());
    }

    @ExceptionHandler(ClientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleClientNotFound(ClientNotFoundException ex, Model model) {
        return renderError(model, 404, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        return renderError(model, 400, ex.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleConfigurationError(ConfigurationException ex, Model model) {
        return renderError(model, 500, ex.getMessage(), true);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        return renderError(model, 500, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unhandled UI exception", ex);
        return renderError(model, 500, "An unexpected error occurred");
    }

    private String renderError(Model model, int statusCode, String message) {
        return renderError(model, statusCode, message, false);
    }

    private String renderError(Model model, int statusCode, String message, boolean isConfigError) {
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", message);
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("isConfigError", isConfigError);
        return "error";
    }
}
