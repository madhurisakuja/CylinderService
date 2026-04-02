package com.cylindertrack.app.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to home redirects to login")
    void unauthenticated_home_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/newhome"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login**"));
    }

    @Test
    @DisplayName("Unauthenticated request to cylinder entry redirects to login")
    void unauthenticated_cylinderEntry_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/newCylinderEntryF"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Unauthenticated request to history redirects to login")
    void unauthenticated_history_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/CylinderHistoryF"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Login page is publicly accessible")
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Static CSS resources are publicly accessible")
    void staticCss_isPublic() throws Exception {
        mockMvc.perform(get("/css/common.css"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated export endpoint redirects to login")
    void unauthenticated_export_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/exportF").param("cylinderNo", "101"))
                .andExpect(status().is3xxRedirection());
    }
}
