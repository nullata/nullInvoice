// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.PartyProfileRepository;
import com.nullinvoice.service.PartyProfileService;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PartyUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartyProfileRepository partyRepository;

    @Autowired
    private PartyProfileService partyProfileService;

    @Test
    void createSupplier_persistsAndRedirects() throws Exception {
        // submit a minimal, valid supplier form and expect a redirect on success
        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Acme Supplies")
                        .param("taxId", "BG123456789")
                        .param("addressLine1", "123 Main St")
                        .param("city", "Sofia")
                        .param("country", "BG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers?saved=1"));

        // verify that the supplier is stored as an active (not deleted) party
        List<Parties> suppliers = partyRepository.findAllByRoleAndDeletedFalseOrderByNameAsc(PartyProfileService.ROLE_SUPPLIER);
        assertThat(suppliers).hasSize(1);
        assertThat(suppliers.get(0).getName()).isEqualTo("Acme Supplies");
    }

    @Test
    void createClient_persistsAndRedirects() throws Exception {
        // submit a minimal, valid client form and expect a redirect on success
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Bravo Client")
                        .param("taxId", "BG987654321")
                        .param("addressLine1", "456 Side St")
                        .param("city", "Plovdiv")
                        .param("country", "BG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients?saved=1"));

        // berify that the client is stored as an active (not deleted) party
        List<Parties> clients = partyRepository.findAllByRoleAndDeletedFalseOrderByNameAsc(PartyProfileService.ROLE_CLIENT);
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).getName()).isEqualTo("Bravo Client");
    }

    @Test
    void editSupplier_updatesExistingRecord() throws Exception {
        // create a supplier up front so we can update it through the UI endpoint
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("Original Supplier", "BG111222333"));

        // post a modified supplier with the same ID, which should update the record
        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", supplier.getId().toString())
                        .param("name", "Updated Supplier")
                        .param("taxId", "BG111222333")
                        .param("addressLine1", "987 Updated Rd")
                        .param("city", "Varna")
                        .param("country", "BG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers?saved=1"));

        // confirm the update landed in the database.
        Parties updated = partyRepository.findById(supplier.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Supplier");
        assertThat(updated.getAddressLine1()).isEqualTo("987 Updated Rd");
    }

    @Test
    void editClient_updatesExistingRecord() throws Exception {
        // create a client up front so we can update it through the UI endpoint
        Parties client = partyProfileService.saveClientProfile(null, buildClientDto("Original Client", "BG444555666"));

        // post a modified client with the same ID, which should update the record
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", client.getId().toString())
                        .param("name", "Updated Client")
                        .param("taxId", "BG444555666")
                        .param("addressLine1", "222 Update Blvd")
                        .param("city", "Burgas")
                        .param("country", "BG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients?saved=1"));

        // confirm the update landed in the database
        Parties updated = partyRepository.findById(client.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Client");
        assertThat(updated.getAddressLine1()).isEqualTo("222 Update Blvd");
    }

    @Test
    void deleteSupplier_marksDeleted() throws Exception {
        // create a supplier, then delete it via the UI endpoint
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("Delete Supplier", "BG999111222"));

        mockMvc.perform(post("/suppliers/{id}/delete", supplier.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers?deleted=1"));

        // validate that the record still exists but is soft-deleted
        Parties deleted = partyRepository.findById(supplier.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();

        Optional<Parties> active = partyRepository.findByIdAndRoleAndDeletedFalse(supplier.getId(), PartyProfileService.ROLE_SUPPLIER);
        assertThat(active).isEmpty();
    }

    @Test
    void deleteClient_marksDeleted() throws Exception {
        // create a client, then delete it via the UI endpoint
        Parties client = partyProfileService.saveClientProfile(null, buildClientDto("Delete Client", "BG777888999"));

        mockMvc.perform(post("/clients/{id}/delete", client.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients?deleted=1"));

        // validate that the record still exists but is soft-deleted
        Parties deleted = partyRepository.findById(client.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();

        Optional<Parties> active = partyRepository.findByIdAndRoleAndDeletedFalse(client.getId(), PartyProfileService.ROLE_CLIENT);
        assertThat(active).isEmpty();
    }

    @Test
    void createSupplier_missingRequiredFields_returnsValidationErrors() throws Exception {
        // submit a supplier with missing required fields to exercise validation
        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "")
                        .param("addressLine1", "")
                        .param("city", "")
                        .param("country", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("supplier"))
                .andExpect(model().attributeHasFieldErrors("supplier", "name", "addressLine1", "city", "country"));

        // ensure nothing was created as a side effect of the failed validation
        List<Parties> suppliers = partyRepository.findAllByRoleAndDeletedFalseOrderByNameAsc(PartyProfileService.ROLE_SUPPLIER);
        assertThat(suppliers).isEmpty();
    }

    @Test
    void createSupplier_duplicateTaxId_returnsError() throws Exception {
        // seed a supplier with a tax ID so the second submit triggers uniqueness checks
        partyProfileService.saveSupplierProfile(null, buildSupplierDto("Dup Supplier", "BG121212121"));

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Dup Supplier")
                        .param("taxId", "BG121212121")
                        .param("addressLine1", "123 Dup St")
                        .param("city", "Sofia")
                        .param("country", "BG"))
                .andExpect(status().isOk())
                .andExpect(view().name("supplier"))
                .andExpect(model().attributeExists("error"));

        // confirm that the duplicate record was not inserted
        List<Parties> suppliers = partyRepository.findAllByRoleAndDeletedFalseOrderByNameAsc(PartyProfileService.ROLE_SUPPLIER);
        assertThat(suppliers).hasSize(1);
    }

    @Test
    void createClient_duplicateTaxId_returnsError() throws Exception {
        // seed a client with a tax ID so the second submit triggers uniqueness checks
        partyProfileService.saveClientProfile(null, buildClientDto("Dup Client", "BG343434343"));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Dup Client")
                        .param("taxId", "BG343434343")
                        .param("addressLine1", "321 Dup St")
                        .param("city", "Sofia")
                        .param("country", "BG"))
                .andExpect(status().isOk())
                .andExpect(view().name("client-form"))
                .andExpect(model().attributeExists("error"));

        // confirm that the duplicate record was not inserted
        List<Parties> clients = partyRepository.findAllByRoleAndDeletedFalseOrderByNameAsc(PartyProfileService.ROLE_CLIENT);
        assertThat(clients).hasSize(1);
    }

    private PartyDto buildSupplierDto(String name, String taxId) {
        PartyDto dto = new PartyDto();
        dto.setName(name);
        dto.setTaxId(taxId);
        dto.setAddressLine1("1 Supply Lane");
        dto.setCity("Sofia");
        dto.setCountry("BG");
        return dto;
    }

    private PartyDto buildClientDto(String name, String taxId) {
        PartyDto dto = new PartyDto();
        dto.setName(name);
        dto.setTaxId(taxId);
        dto.setAddressLine1("2 Client Road");
        dto.setCity("Plovdiv");
        dto.setCountry("BG");
        return dto;
    }
}
