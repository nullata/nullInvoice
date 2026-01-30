// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.error.ClientNotFoundException;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.PartyProfileRepository;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PartyProfileService client lookup and search validation
 * methods
 */
@ExtendWith(MockitoExtension.class)
class PartyProfileServiceTest {

    @Mock
    private PartyProfileRepository repository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceTemplateService invoiceTemplateService;

    private PartyProfileService service;

    @BeforeEach
    void setUp() {
        service = new PartyProfileService(repository, invoiceRepository, invoiceTemplateService);
    }

    // === findClientByTaxOrVatRequired ===
    @Test
    void findClientByTaxOrVatRequired_bothBlank_throwsException() {
        // at least one of taxId or vatId must be provided
        assertThatThrownBy(() -> service.findClientByTaxOrVatRequired(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taxId or vatId is required");

        assertThatThrownBy(() -> service.findClientByTaxOrVatRequired("", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taxId or vatId is required");

        assertThatThrownBy(() -> service.findClientByTaxOrVatRequired("  ", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taxId or vatId is required");
    }

    @Test
    void findClientByTaxOrVatRequired_clientNotFound_throwsClientNotFoundException() {
        // valid input but no matching client should throw ClientNotFoundException
        when(repository.findFirstByRoleAndTaxId(eq(PartyProfileService.ROLE_CLIENT), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findClientByTaxOrVatRequired("BG123456", null))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessage("client not found");
    }

    @Test
    void findClientByTaxOrVatRequired_withTaxId_returnsClient() {
        // lookup by taxId should return matching client
        Parties client = createClient("Test Client", "BG123456", null);
        when(repository.findFirstByRoleAndTaxId(PartyProfileService.ROLE_CLIENT, "BG123456"))
                .thenReturn(Optional.of(client));

        PartyDto result = service.findClientByTaxOrVatRequired("BG123456", null);

        assertThat(result.getName()).isEqualTo("Test Client");
        assertThat(result.getTaxId()).isEqualTo("BG123456");
    }

    @Test
    void findClientByTaxOrVatRequired_withVatId_returnsClient() {
        // lookup by vatId should return matching client
        Parties client = createClient("Test Client", null, "BG123456789");
        when(repository.findFirstByRoleAndVatId(PartyProfileService.ROLE_CLIENT, "BG123456789"))
                .thenReturn(Optional.of(client));

        PartyDto result = service.findClientByTaxOrVatRequired(null, "BG123456789");

        assertThat(result.getName()).isEqualTo("Test Client");
        assertThat(result.getVatId()).isEqualTo("BG123456789");
    }

    @Test
    void findClientByTaxOrVatRequired_taxIdTakesPrecedence() {
        // when both are provided, taxId should be checked first
        Parties client = createClient("Tax Client", "BG111", null);
        when(repository.findFirstByRoleAndTaxId(PartyProfileService.ROLE_CLIENT, "BG111"))
                .thenReturn(Optional.of(client));

        PartyDto result = service.findClientByTaxOrVatRequired("BG111", "BG222");

        assertThat(result.getName()).isEqualTo("Tax Client");
    }

    // === searchClientsWithMinLength ===
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"a"})
    void searchClientsWithMinLength_queryTooShort_returnsEmptyList(String query) {
        // queries shorter than minimum length should return empty without hitting DB
        List<PartyDto> result = service.searchClientsWithMinLength(query, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void searchClientsWithMinLength_queryMeetsMinLength_returnsResults() {
        // query meeting minimum length should perform search
        Parties client = createClient("Acme Corp", "BG123", null);
        when(repository.searchByRoleAndQueryAndDeletedFalse(PartyProfileService.ROLE_CLIENT, "Ac"))
                .thenReturn(List.of(client));

        List<PartyDto> result = service.searchClientsWithMinLength("Ac", 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Acme Corp");
    }

    @Test
    void searchClientsWithMinLength_queryExceedsMinLength_returnsResults() {
        // longer queries should still hit the repository and return matches
        Parties client = createClient("Acme Corp", "BG123", null);
        when(repository.searchByRoleAndQueryAndDeletedFalse(PartyProfileService.ROLE_CLIENT, "Acme"))
                .thenReturn(List.of(client));

        List<PartyDto> result = service.searchClientsWithMinLength("Acme", 2);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchClientsWithMinLength_noResults_returnsEmptyList() {
        // empty repository results should pass through as empty lists
        when(repository.searchByRoleAndQueryAndDeletedFalse(PartyProfileService.ROLE_CLIENT, "xyz"))
                .thenReturn(List.of());

        List<PartyDto> result = service.searchClientsWithMinLength("xyz", 2);

        assertThat(result).isEmpty();
    }

    @Test
    void searchClientsWithMinLength_customMinLength_respectsIt() {
        // custom minimum length should be honored
        List<PartyDto> result = service.searchClientsWithMinLength("abc", 5);

        assertThat(result).isEmpty(); // "abc" is shorter than 5 chars
    }

    // === setSupplierDefaultTemplate(String) ===
    @Test
    void setSupplierDefaultTemplate_withStringNull_clearsTemplate() {
        // null string should clear the supplier's default template
        Parties supplier = createSupplier("Supplier");
        when(repository.findByIdAndRoleAndDeletedFalse(1L, PartyProfileService.ROLE_SUPPLIER))
                .thenReturn(Optional.of(supplier));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setSupplierDefaultTemplate(1L, (String) null);

        assertThat(supplier.getDefaultTemplateId()).isNull();
    }

    @Test
    void setSupplierDefaultTemplate_withEmptyString_clearsTemplate() {
        // empty string should clear the supplier's default template
        Parties supplier = createSupplier("Supplier");
        when(repository.findByIdAndRoleAndDeletedFalse(1L, PartyProfileService.ROLE_SUPPLIER))
                .thenReturn(Optional.of(supplier));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setSupplierDefaultTemplate(1L, "");

        assertThat(supplier.getDefaultTemplateId()).isNull();
    }

    @Test
    void setSupplierDefaultTemplate_withBlankString_clearsTemplate() {
        // whitespace-only string should clear the supplier's default template
        Parties supplier = createSupplier("Supplier");
        when(repository.findByIdAndRoleAndDeletedFalse(1L, PartyProfileService.ROLE_SUPPLIER))
                .thenReturn(Optional.of(supplier));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setSupplierDefaultTemplate(1L, "   ");

        assertThat(supplier.getDefaultTemplateId()).isNull();
    }

    private Parties createClient(String name, String taxId, String vatId) {
        Parties party = new Parties();
        party.setId(1L);
        party.setRole(PartyProfileService.ROLE_CLIENT);
        party.setName(name);
        party.setTaxId(taxId);
        party.setVatId(vatId);
        party.setAddressLine1("123 Main St");
        party.setCity("Sofia");
        party.setCountry("BG");
        return party;
    }

    private Parties createSupplier(String name) {
        Parties party = new Parties();
        party.setId(1L);
        party.setRole(PartyProfileService.ROLE_SUPPLIER);
        party.setName(name);
        party.setAddressLine1("456 Supply Rd");
        party.setCity("Sofia");
        party.setCountry("BG");
        return party;
    }
}
