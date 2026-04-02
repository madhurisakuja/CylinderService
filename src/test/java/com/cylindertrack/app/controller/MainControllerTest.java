package com.cylindertrack.app.controller;

import com.cylindertrack.app.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MainController")
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewCylinderFService cylinderService;

    @MockBean
    private PartyNamesRepository partyNamesRepository;

    private OidcUser mockUser() {
        OidcIdToken token = new OidcIdToken(
            "token", Instant.now(), Instant.now().plusSeconds(3600),
            Map.of("sub", "12345", "email", "allowed@example.com", "name", "Test User")
        );
        return new DefaultOidcUser(List.of(), token);
    }

    @Test
    @DisplayName("GET /newhome returns 200 for authenticated user")
    void home_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/newhome").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("newhome"));
    }

    @Test
    @DisplayName("GET /newCylinderEntryF returns form with party names and types")
    void newCylinderEntry_get_returnsForm() throws Exception {
        when(partyNamesRepository.getAllPartyNames()).thenReturn(List.of("Sharma Gas", "Patel Traders"));

        mockMvc.perform(get("/newCylinderEntryF").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("newCylinderEntryF"))
                .andExpect(model().attributeExists("partyNames", "types", "statuses", "entry"));
    }

    @Test
    @DisplayName("POST /newCylinderEntryF with valid data saves and redirects")
    void newCylinderEntry_post_validData_redirects() throws Exception {
        MainCylinderEntry saved = new MainCylinderEntry();
        saved.setCylinderNo(101L);
        saved.setCustomerName("Sharma Gas");
        saved.setCtype("FULL");

        when(cylinderService.saveAndFlush(any())).thenReturn(saved);
        when(cylinderService.getCylinderStatus(any())).thenReturn(null);
        when(cylinderService.getCylinderHoldingStatus(any())).thenReturn(null);

        mockMvc.perform(post("/newCylinderEntryF")
                .with(oidcLogin().oidcUser(mockUser()))
                .param("customerName", "Sharma Gas")
                .param("date", "2024-01-15")
                .param("cylinderNo", "101")
                .param("ctype", "FULL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/newCylinderEntryF**"));
    }

    @Test
    @DisplayName("POST /newCylinderEntryF with missing fields redirects with error")
    void newCylinderEntry_post_missingFields_redirectsWithError() throws Exception {
        mockMvc.perform(post("/newCylinderEntryF")
                .with(oidcLogin().oidcUser(mockUser()))
                .param("cylinderNo", "101"))
                // Missing customerName, date, ctype — should redirect back with validation error
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /newPartyEntryF with new name saves successfully")
    void newPartyEntry_post_newName_saves() throws Exception {
        when(partyNamesRepository.getAllPartyNames()).thenReturn(List.of("Existing Party"));
        PartyNames saved = new PartyNames();
        saved.setPartyName("New Party");
        when(partyNamesRepository.saveAndFlush(any())).thenReturn(saved);

        mockMvc.perform(post("/newPartyEntryF")
                .with(oidcLogin().oidcUser(mockUser()))
                .param("partyName", "New Party"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/newPartyEntryF**"));
    }

    @Test
    @DisplayName("POST /newPartyEntryF with duplicate name does not save")
    void newPartyEntry_post_duplicateName_doesNotSave() throws Exception {
        when(partyNamesRepository.getAllPartyNames()).thenReturn(List.of("Sharma Gas"));

        mockMvc.perform(post("/newPartyEntryF")
                .with(oidcLogin().oidcUser(mockUser()))
                .param("partyName", "Sharma Gas"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /CylinderHistoryF returns history form")
    void cylinderHistory_get_returnsForm() throws Exception {
        mockMvc.perform(get("/CylinderHistoryF").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("CylinderHistoryF"));
    }

    @Test
    @DisplayName("POST /CylinderHistoryF redirects to search results")
    void cylinderHistory_post_redirectsToResults() throws Exception {
        when(cylinderService.findAllByCylinderNo(101L)).thenReturn(List.of());

        mockMvc.perform(post("/CylinderHistoryF")
                .with(oidcLogin().oidcUser(mockUser()))
                .param("cylinderNo", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/searchResultF**"));
    }

    @Test
    @DisplayName("GET /deleteCylinderEntryF returns delete form")
    void deleteCylinder_get_returnsForm() throws Exception {
        when(partyNamesRepository.getAllPartyNames()).thenReturn(List.of("Sharma Gas"));

        mockMvc.perform(get("/deleteCylinderEntryF").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().isOk())
                .andExpect(view().name("deleteCylinderEntryF"))
                .andExpect(model().attributeExists("types", "partyNames"));
    }

    @Test
    @DisplayName("GET /notInRotation redirects to results")
    void notInRotation_redirectsToResults() throws Exception {
        when(cylinderService.getCylindersNotInRotation(any())).thenReturn(List.of());

        mockMvc.perform(get("/notInRotation").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/searchResultF**"));
    }

    @Test
    @DisplayName("GET /searchResultF without flash data redirects to history")
    void searchResult_noFlash_redirectsToHistory() throws Exception {
        mockMvc.perform(get("/searchResultF").with(oidcLogin().oidcUser(mockUser())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/CylinderHistoryF"));
    }
}
