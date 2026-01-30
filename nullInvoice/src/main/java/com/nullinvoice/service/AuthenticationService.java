// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.ApiKeyDto;
import com.nullinvoice.dto.ApiKeyGeneratedDto;
import com.nullinvoice.entity.ApiKeys;
import com.nullinvoice.entity.Users;
import com.nullinvoice.repository.ApiKeyRepository;
import com.nullinvoice.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean hasAdminUser() {
        return userRepository.count() > 0;
    }

    public String getPasswordHint(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getPasswordHint)
                .orElse(null);
    }

    @Transactional
    public void createInitialAdmin(String username, String password, String passwordHint) {
        if (hasAdminUser()) {
            throw new IllegalStateException("Admin user already exists");
        }

        Users user = new Users();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordHint(passwordHint);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String passwordHint) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordHint(passwordHint);
        userRepository.save(user);
    }

    @Transactional
    public ApiKeyGeneratedDto generateApiKey(String description) {
        String apiKey = UUID.randomUUID().toString();
        String keySuffix = apiKey.substring(apiKey.length() - 8);

        ApiKeys apiKeyEntity = new ApiKeys();
        apiKeyEntity.setKeyHash(passwordEncoder.encode(apiKey));
        apiKeyEntity.setKeySuffix(keySuffix);
        apiKeyEntity.setDescription(description);
        apiKeyEntity.setEnabled(true);

        apiKeyEntity = apiKeyRepository.save(apiKeyEntity);

        ApiKeyDto dto = mapToDto(apiKeyEntity);
        return new ApiKeyGeneratedDto(apiKey, dto);
    }

    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        List<ApiKeys> enabledKeys = apiKeyRepository.findAllByEnabledTrueOrderByCreatedAtDesc();

        for (ApiKeys key : enabledKeys) {
            if (passwordEncoder.matches(apiKey, key.getKeyHash())) {
                updateLastUsed(key);
                return true;
            }
        }

        return false;
    }

    @Transactional
    protected void updateLastUsed(ApiKeys key) {
        key.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(key);
    }

    public List<ApiKeyDto> listApiKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeApiKey(Long keyId) {
        ApiKeys key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        key.setEnabled(false);
        apiKeyRepository.save(key);
    }

    private ApiKeyDto mapToDto(ApiKeys entity) {
        ApiKeyDto dto = new ApiKeyDto();
        dto.setId(entity.getId());
        dto.setKeySuffix(entity.getKeySuffix());
        dto.setDescription(entity.getDescription());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastUsedAt(entity.getLastUsedAt());
        return dto;
    }
}
