package dev.noby.history.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class PackageHistoryControllerSecurityTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/packages/42/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAdminOrSupervisorGroup() throws Exception {
        mvc.perform(get("/api/v1/packages/42/history")
                        .with(jwt().jwt(token -> token.claim("cognito:groups", List.of("OTHER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permitsSupervisorAndValidatesPackageId() throws Exception {
        mvc.perform(get("/api/v1/packages/42/history")
                        .with(jwt().jwt(token -> token.claim("cognito:groups", List.of("SUPERVISOR")))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/packages/0/history")
                        .with(jwt().jwt(token -> token.claim("cognito:groups", List.of("ADMIN")))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }
}
