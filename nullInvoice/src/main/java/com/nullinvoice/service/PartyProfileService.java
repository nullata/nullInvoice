// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.error.ClientNotFoundException;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.PartyProfileRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartyProfileService {

    public static final String ROLE_SUPPLIER = "SUPPLIER";
    public static final String ROLE_CLIENT = "CLIENT";

    private final PartyProfileRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceTemplateService invoiceTemplateService;

    public Parties upsertParty(PartyDto party, String role) {
        if (party == null) {
            throw new IllegalArgumentException(role.toLowerCase() + " party is required");
        }

        Optional<Parties> existing = findExisting(role, party);
        Parties profile = existing.orElseGet(Parties::new);
        applyParty(profile, party, role);

        return repository.save(profile);
    }

    public Parties saveSupplierProfile(PartyDto supplier) {
        Optional<Parties> existing = findExisting(ROLE_SUPPLIER, supplier);
        if (existing.isEmpty()) {
            existing = repository.findFirstByRoleOrderByIdAsc(ROLE_SUPPLIER);
        }
        validateSupplierUniqueness(supplier, existing.map(Parties::getId).orElse(null));
        Parties profile = existing.orElseGet(Parties::new);
        applyParty(profile, supplier, ROLE_SUPPLIER);
        return repository.save(profile);
    }

    public Optional<PartyDto> findSupplier() {
        return repository.findFirstByRoleAndDeletedFalseOrderByIdAsc(ROLE_SUPPLIER).map(this::toDto);
    }

    public Optional<PartyDto> findClientByTaxOrVat(String taxId, String vatId) {
        String trimmedTaxId = trimToNull(taxId);
        if (trimmedTaxId != null) {
            return repository.findFirstByRoleAndTaxId(ROLE_CLIENT, trimmedTaxId).map(this::toDto);
        }
        String trimmedVatId = trimToNull(vatId);
        if (trimmedVatId != null) {
            return repository.findFirstByRoleAndVatId(ROLE_CLIENT, trimmedVatId).map(this::toDto);
        }
        return Optional.empty();
    }

    /**
     * Finds client by tax or VAT id, requiring at least one to be provided
     *
     * @param taxId
     * @param vatId
     * @return
     * @throws IllegalArgumentException if both taxId and vatId are blank
     * @throws ClientNotFoundException if no client is found
     */
    public PartyDto findClientByTaxOrVatRequired(String taxId, String vatId) {
        if ((taxId == null || taxId.isBlank()) && (vatId == null || vatId.isBlank())) {
            throw new IllegalArgumentException("taxId or vatId is required");
        }
        return findClientByTaxOrVat(taxId, vatId)
                .orElseThrow(() -> new ClientNotFoundException("client not found"));
    }

    /**
     * Searches clients with minimum query length validation.Returns empty list
     * if query is too short (less than 2 characters).
     *
     * @param query
     * @param minLength
     * @return
     */
    public List<PartyDto> searchClientsWithMinLength(String query, int minLength) {
        if (query == null || query.length() < minLength) {
            return List.of();
        }
        return searchClients(query);
    }

    // multi-supplier methods
    @Transactional(readOnly = true)
    public List<PartyDto> listSuppliers() {
        return repository.findAllByRoleAndDeletedFalseOrderByNameAsc(ROLE_SUPPLIER)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PartyDto> findSupplierById(Long id) {
        return repository.findByIdAndRoleAndDeletedFalse(id, ROLE_SUPPLIER).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<Parties> findSupplierEntityById(Long id) {
        return repository.findByIdAndRoleAndDeletedFalse(id, ROLE_SUPPLIER);
    }

    @Transactional(readOnly = true)
    public Optional<Parties> findClientEntityById(Long id) {
        return repository.findByIdAndRoleAndDeletedFalse(id, ROLE_CLIENT);
    }

    @Transactional
    public Parties lockPartyForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("party not found: " + id));
    }

    @Transactional
    public Parties lockSupplierForUpdate(Long id) {
        return repository.findByIdAndRoleForUpdate(id, ROLE_SUPPLIER)
                .orElseThrow(() -> new IllegalArgumentException("supplier not found"));
    }

    @Transactional
    public Parties saveSupplierProfile(Long id, PartyDto supplier) {
        validateSupplierUniqueness(supplier, id);
        Parties profile;
        if (id != null) {
            profile = repository.findByIdAndRoleAndDeletedFalse(id, ROLE_SUPPLIER).orElseGet(Parties::new);
        } else {
            profile = new Parties();
        }
        applyParty(profile, supplier, ROLE_SUPPLIER);
        applyLocalization(profile, supplier);
        return repository.save(profile);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        repository.findByIdAndRoleAndDeletedFalse(id, ROLE_SUPPLIER).ifPresent(supplier -> {
            supplier.setDeleted(true);
            supplier.setUpdatedAt(new Date());
            repository.save(supplier);
        });
    }

    @Transactional(readOnly = true)
    public long getSupplierInvoiceCount(Long supplierId) {
        return invoiceRepository.countBySupplierPartyId_Id(supplierId);
    }

    // client management methods
    @Transactional(readOnly = true)
    public List<PartyDto> listClients() {
        return repository.findAllByRoleAndDeletedFalseOrderByNameAsc(ROLE_CLIENT)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PartyDto> searchClients(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return repository.searchByRoleAndQueryAndDeletedFalse(ROLE_CLIENT, query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PartyDto> searchSuppliers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return repository.searchByRoleAndQueryAndDeletedFalse(ROLE_SUPPLIER, query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PartyDto> findClientById(Long id) {
        return repository.findByIdAndRoleAndDeletedFalse(id, ROLE_CLIENT).map(this::toDto);
    }

    @Transactional
    public Parties saveClientProfile(Long id, PartyDto client) {
        validateClientUniqueness(client, id);
        Parties profile;
        if (id != null) {
            profile = repository.findByIdAndRoleAndDeletedFalse(id, ROLE_CLIENT).orElseGet(Parties::new);
        } else {
            profile = new Parties();
        }
        applyParty(profile, client, ROLE_CLIENT);
        return repository.save(profile);
    }

    @Transactional
    public void deleteClient(Long id) {
        repository.findByIdAndRoleAndDeletedFalse(id, ROLE_CLIENT).ifPresent(client -> {
            client.setDeleted(true);
            client.setUpdatedAt(new Date());
            repository.save(client);
        });
    }

    @Transactional(readOnly = true)
    public long getClientInvoiceCount(Long clientId) {
        return invoiceRepository.countByClientPartyId_Id(clientId);
    }

    // count methods for dashboard
    @Transactional(readOnly = true)
    public long countSuppliers() {
        return repository.countByRoleAndDeletedFalse(ROLE_SUPPLIER);
    }

    @Transactional(readOnly = true)
    public long countClients() {
        return repository.countByRoleAndDeletedFalse(ROLE_CLIENT);
    }

    private Optional<Parties> findExisting(String role, PartyDto party) {
        String taxId = trimToNull(party.getTaxId());
        if (taxId != null) {
            return repository.findFirstByRoleAndTaxId(role, taxId);
        }
        String vatId = trimToNull(party.getVatId());
        if (vatId != null) {
            return repository.findFirstByRoleAndVatId(role, vatId);
        }
        return Optional.empty();
    }

    private void validateClientUniqueness(PartyDto client, Long excludeId) {
        if (client == null) {
            return;
        }
        String name = trimToNull(client.getName());
        if (name == null) {
            return;
        }
        String taxId = trimToNull(client.getTaxId());
        String vatId = trimToNull(client.getVatId());
        if (taxId == null && vatId == null) {
            return;
        }
        Optional<Parties> existing = repository.findFirstByRoleAndDeletedFalseAndNameAndTaxOrVat(
                ROLE_CLIENT, name, taxId, vatId, excludeId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A client with same name and tax/vat id already exists");
        }
    }

    private void validateSupplierUniqueness(PartyDto supplier, Long excludeId) {
        if (supplier == null) {
            return;
        }
        String name = trimToNull(supplier.getName());
        if (name == null) {
            return;
        }
        String taxId = trimToNull(supplier.getTaxId());
        String vatId = trimToNull(supplier.getVatId());
        if (taxId == null && vatId == null) {
            return;
        }
        Optional<Parties> existing = repository.findFirstByRoleAndDeletedFalseAndNameAndTaxOrVat(
                ROLE_SUPPLIER, name, taxId, vatId, excludeId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A supplier with same name and tax/vat id already exists");
        }
    }

    private void applyParty(Parties profile, PartyDto party, String role) {
        boolean isNew = profile.getId() == null;
        profile.setRole(role);
        profile.setName(party.getName());
        profile.setTaxId(trimToNull(party.getTaxId()));
        profile.setVatId(trimToNull(party.getVatId()));
        profile.setAddressLine1(party.getAddressLine1());
        profile.setAddressLine2(trimToNull(party.getAddressLine2()));
        profile.setCity(party.getCity());
        profile.setRegion(trimToNull(party.getRegion()));
        profile.setPostalCode(trimToNull(party.getPostalCode()));
        profile.setCountry(party.getCountry());
        profile.setEmail(trimToNull(party.getEmail()));
        profile.setPhone(trimToNull(party.getPhone()));
        if (isNew) {
            profile.setCreatedAt(new Date());
        }
        profile.setUpdatedAt(new Date());
    }

    private PartyDto toDto(Parties profile) {
        PartyDto dto = new PartyDto();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setTaxId(profile.getTaxId());
        dto.setVatId(profile.getVatId());
        dto.setAddressLine1(profile.getAddressLine1());
        dto.setAddressLine2(profile.getAddressLine2());
        dto.setCity(profile.getCity());
        dto.setRegion(profile.getRegion());
        dto.setPostalCode(profile.getPostalCode());
        dto.setCountry(profile.getCountry());
        dto.setEmail(profile.getEmail());
        dto.setPhone(profile.getPhone());
        // localization fields
        dto.setLocaleCode(profile.getLocaleCode());
        dto.setLocaleLanguage(profile.getLocaleLanguage());
        dto.setLocaleCountry(profile.getLocaleCountry());
        dto.setCurrencyCode(profile.getCurrencyCode());
        dto.setDatePattern(profile.getDatePattern());
        dto.setInvoiceStartNumber(profile.getInvoiceStartNumber());
        dto.setInvoicePrefix(profile.getInvoicePrefix());
        dto.setInvoiceNumberDigits(profile.getInvoiceNumberDigits());
        dto.setLastInvoiceNumber(profile.getLastInvoiceNumber());
        dto.setDefaultTemplateId(profile.getDefaultTemplateId() != null ? profile.getDefaultTemplateId().getId() : null);
        dto.setDefaultTaxRate(profile.getDefaultTaxRate());
        return dto;
    }

    private void applyLocalization(Parties profile, PartyDto party) {
        profile.setLocaleCode(trimToNull(party.getLocaleCode()));
        profile.setLocaleLanguage(trimToNull(party.getLocaleLanguage()));
        profile.setLocaleCountry(trimToNull(party.getLocaleCountry()));
        profile.setCurrencyCode(trimToNull(party.getCurrencyCode()));
        profile.setDatePattern(trimToNull(party.getDatePattern()));
        profile.setInvoiceStartNumber(party.getInvoiceStartNumber());
        profile.setInvoicePrefix(trimToNull(party.getInvoicePrefix()));
        profile.setInvoiceNumberDigits(party.getInvoiceNumberDigits());
        profile.setDefaultTaxRate(party.getDefaultTaxRate());
    }

    @Transactional
    public void setSupplierDefaultTemplate(Long supplierId, Long templateId) {
        Parties supplier = repository.findByIdAndRoleAndDeletedFalse(supplierId, ROLE_SUPPLIER)
                .orElseThrow(() -> new IllegalArgumentException("supplier not found"));
        if (templateId == null) {
            supplier.setDefaultTemplateId(null);
        } else {
            supplier.setDefaultTemplateId(invoiceTemplateService.getTemplateById(templateId));
        }
        supplier.setUpdatedAt(new Date());
        repository.save(supplier);
    }

    /**
     * Sets supplier default template, parsing the template ID from string
     */
    @Transactional
    public void setSupplierDefaultTemplate(Long supplierId, String templateIdString) {
        Long templateId = null;
        if (templateIdString != null && !templateIdString.isBlank()) {
            templateId = Long.valueOf(templateIdString);
        }
        setSupplierDefaultTemplate(supplierId, templateId);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
