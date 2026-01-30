// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.SetupFormDto;
import com.nullinvoice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Hidden
public class AuthController {

    private final AuthenticationService authenticationService;
    private final MessageSource messageSource;

    @GetMapping("/setup")
    public String showSetup(Model model) {
        if (authenticationService.hasAdminUser()) {
            return "redirect:/";
        }

        if (!model.containsAttribute("setupForm")) {
            model.addAttribute("setupForm", new SetupFormDto());
        }

        return "setup";
    }

    @PostMapping("/setup")
    public String processSetup(@Valid @ModelAttribute("setupForm") SetupFormDto setupForm,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (authenticationService.hasAdminUser()) {
            return "redirect:/";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.setupForm", result);
            redirectAttributes.addFlashAttribute("setupForm", setupForm);
            return "redirect:/setup";
        }

        if (!setupForm.getPassword().equals(setupForm.getPasswordRepeat())) {
            redirectAttributes.addFlashAttribute("setupForm", setupForm);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("setup.message.password.mismatch", null, LocaleContextHolder.getLocale()));
            return "redirect:/setup";
        }

        try {
            authenticationService.createInitialAdmin(
                    setupForm.getUsername(),
                    setupForm.getPassword(),
                    setupForm.getPasswordHint()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("setup.message.success", null, LocaleContextHolder.getLocale()));
            return "redirect:/login?setup";

        } catch (NoSuchMessageException e) {
            redirectAttributes.addFlashAttribute("setupForm", setupForm);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("setup.message.failed", null, LocaleContextHolder.getLocale()));
            return "redirect:/setup";
        }
    }

    @GetMapping("/login")
    public String showLogin(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String setup,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("login.message.invalid", null, LocaleContextHolder.getLocale()));
        }

        if (logout != null) {
            model.addAttribute("successMessage",
                    messageSource.getMessage("login.message.logout", null, LocaleContextHolder.getLocale()));
        }

        if (setup != null) {
            model.addAttribute("successMessage",
                    messageSource.getMessage("setup.message.success.login", null, LocaleContextHolder.getLocale()));
        }

        return "login";
    }

    @GetMapping("/login/hint")
    @ResponseBody
    public String getPasswordHint(@RequestParam String username) {
        String hint = authenticationService.getPasswordHint(username);
        if (hint != null && !hint.isBlank()) {
            return hint;
        }
        return messageSource.getMessage("login.hint.none", null, LocaleContextHolder.getLocale());
    }
}
