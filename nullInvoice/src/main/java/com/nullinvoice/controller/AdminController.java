// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.ApiKeyDto;
import com.nullinvoice.dto.ApiKeyGeneratedDto;
import com.nullinvoice.dto.PasswordChangeDto;
import com.nullinvoice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Hidden
public class AdminController {

    private final AuthenticationService authenticationService;
    private final MessageSource messageSource;

    @GetMapping
    public String showAdmin(Model model, Authentication authentication) {
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute("passwordChangeForm", new PasswordChangeDto());
        }

        List<ApiKeyDto> apiKeys = authenticationService.listApiKeys();
        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("username", authentication.getName());

        return "admin";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeForm") PasswordChangeDto passwordChangeForm,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChangeForm", result);
            redirectAttributes.addFlashAttribute("passwordChangeForm", passwordChangeForm);
            return "redirect:/admin";
        }

        if (!passwordChangeForm.getNewPassword().equals(passwordChangeForm.getNewPasswordRepeat())) {
            redirectAttributes.addFlashAttribute("passwordChangeForm", passwordChangeForm);
            redirectAttributes.addFlashAttribute("passwordError",
                    messageSource.getMessage("admin.message.password.mismatch", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";
        }

        try {
            authenticationService.changePassword(
                    authentication.getName(),
                    passwordChangeForm.getCurrentPassword(),
                    passwordChangeForm.getNewPassword(),
                    passwordChangeForm.getPasswordHint()
            );

            redirectAttributes.addFlashAttribute("passwordSuccess",
                    messageSource.getMessage("admin.message.password.success", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordChangeForm", passwordChangeForm);
            String errorMessage = e.getMessage().contains("Current password")
                    ? messageSource.getMessage("admin.message.password.incorrect", null, LocaleContextHolder.getLocale())
                    : e.getMessage();
            redirectAttributes.addFlashAttribute("passwordError", errorMessage);
            return "redirect:/admin";
        }
    }

    @PostMapping("/api-keys/generate")
    public String generateApiKey(@RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            ApiKeyGeneratedDto generated = authenticationService.generateApiKey(description);

            redirectAttributes.addFlashAttribute("generatedApiKey", generated.getApiKey());
            redirectAttributes.addFlashAttribute("apiKeySuccess",
                    messageSource.getMessage("admin.message.apiKey.generated", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";

        } catch (NoSuchMessageException e) {
            redirectAttributes.addFlashAttribute("apiKeyError",
                    messageSource.getMessage("admin.message.apiKey.generate.failed", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";
        }
    }

    @PostMapping("/api-keys/{id}/revoke")
    public String revokeApiKey(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            authenticationService.revokeApiKey(id);

            redirectAttributes.addFlashAttribute("apiKeySuccess",
                    messageSource.getMessage("admin.message.apiKey.revoked", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";

        } catch (NoSuchMessageException e) {
            redirectAttributes.addFlashAttribute("apiKeyError",
                    messageSource.getMessage("admin.message.apiKey.revoke.failed", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin";
        }
    }
}
