package edu.iiitb.team007.ledger.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.iiitb.team007.ledger.config.LedgerProperties;
import edu.iiitb.team007.ledger.persistence.DatabaseHealth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** GET /health answers 503 "degraded" when the database is unusable - the provider is substituted (FR-6.4, SE-6). */
@WebMvcTest(HealthController.class)
@EnableConfigurationProperties(LedgerProperties.class)
class HealthControllerDegradedTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DatabaseHealth databaseHealth;

    @Test
    void healthIsDegradedWhenDatabaseQueryFails() throws Exception {
        when(databaseHealth.isAvailable()).thenReturn(false);

        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("degraded"))
                .andExpect(jsonPath("$.database").value("unavailable"));
    }
}
