package com.cylindertrack.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NewCylinderFService (repository)")
class CylinderEntryServiceTest {

    @Autowired
    private NewCylinderFService service;

    private Date today;
    private Date yesterday;

    @BeforeEach
    void setUp() {
        service.deleteAll();
        today     = toDate(LocalDate.now());
        yesterday = toDate(LocalDate.now().minusDays(1));

        save("Sharma Gas",    today,     "FULL",  101L);
        save("Sharma Gas",    today,     "FULL",  102L);
        save("Patel Traders", yesterday, "EMPTY", 101L);
    }

    @Test
    @DisplayName("getCylinderStatus returns the most recent entry type")
    void getCylinderStatus_returnsMostRecent() {
        // cylinder 101 last entry was FULL (id is higher)
        String status = service.getCylinderStatus(101L);
        assertThat(status).isEqualTo("FULL");
    }

    @Test
    @DisplayName("getCylinderHoldingStatus returns the most recent customer")
    void getCylinderHoldingStatus_returnsMostRecentCustomer() {
        String customer = service.getCylinderHoldingStatus(101L);
        assertThat(customer).isEqualTo("Sharma Gas");
    }

    @Test
    @DisplayName("findAllByCylinderNo returns history in date ascending order")
    void findAllByCylinderNo_returnsAscendingHistory() {
        List<List<?>> history = service.findAllByCylinderNo(101L);
        assertThat(history).hasSize(2);
        // First row should be the older entry (yesterday = EMPTY)
        assertThat(history.get(0).get(1)).isEqualTo("EMPTY");
        assertThat(history.get(1).get(1)).isEqualTo("FULL");
    }

    @Test
    @DisplayName("findAllByCylinderNo returns empty list for unknown cylinder")
    void findAllByCylinderNo_unknownCylinder_returnsEmpty() {
        List<List<?>> history = service.findAllByCylinderNo(9999L);
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("findByDetails returns matching entry id")
    void findByDetails_returnsMatchingId() {
        List<Long> ids = service.findByDetails(102L, today, "Sharma Gas", "FULL");
        assertThat(ids).hasSize(1);
    }

    @Test
    @DisplayName("findByDetails returns empty for non-matching details")
    void findByDetails_noMatch_returnsEmpty() {
        List<Long> ids = service.findByDetails(102L, yesterday, "Sharma Gas", "FULL");
        assertThat(ids).isEmpty();
    }

    @Test
    @DisplayName("getCylindersNotInRotation returns cylinders idle since given date")
    void getCylindersNotInRotation_returnsIdleCylinders() {
        // cylinder 101 last seen today — should NOT appear as idle since yesterday
        // cylinder 102 last seen today — should NOT appear either
        Date twoDaysAgo = toDate(LocalDate.now().minusDays(2));
        List<List<?>> idle = service.getCylindersNotInRotation(twoDaysAgo);
        // Both cylinders were seen more recently than twoDaysAgo
        assertThat(idle).isEmpty();
    }

    @Test
    @DisplayName("getCylindersNotInRotation includes cylinder not updated since offset")
    void getCylindersNotInRotation_includesStale() {
        // Add a cylinder last updated 20 days ago
        Date oldDate = toDate(LocalDate.now().minusDays(20));
        save("Old Customer", oldDate, "FULL", 999L);

        Date fifteenDaysAgo = toDate(LocalDate.now().minusDays(15));
        List<List<?>> idle = service.getCylindersNotInRotation(fifteenDaysAgo);

        List<Long> idleCylinders = idle.stream()
                .map(row -> (Long) row.get(0))
                .toList();
        assertThat(idleCylinders).contains(999L);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void save(String customer, Date date, String ctype, Long cylinderNo) {
        MainCylinderEntry e = new MainCylinderEntry();
        e.setCustomerName(customer);
        e.setDate(date);
        e.setCtype(ctype);
        e.setCylinderNo(cylinderNo);
        service.saveAndFlush(e);
    }

    private Date toDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
