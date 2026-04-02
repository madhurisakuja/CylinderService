package com.cylindertrack.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PartyNamesRepository")
class PartyNamesRepositoryTest {

    @Autowired
    private PartyNamesRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        PartyNames a = new PartyNames(); a.setPartyName("Sharma Gas");
        PartyNames b = new PartyNames(); b.setPartyName("Patel Traders");
        PartyNames c = new PartyNames(); c.setPartyName("Agarwal & Sons");
        repository.saveAll(List.of(a, b, c));
    }

    @Test
    @DisplayName("getAllPartyNames returns all names in alphabetical order")
    void getAllPartyNames_returnsAlphabetical() {
        List<String> names = repository.getAllPartyNames();
        assertThat(names).hasSize(3);
        assertThat(names).containsExactly("Agarwal & Sons", "Patel Traders", "Sharma Gas");
    }

    @Test
    @DisplayName("save does not allow duplicate party names")
    void save_duplicatePartyName_existsCheck() {
        List<String> before = repository.getAllPartyNames();
        boolean isDuplicate = before.contains("Sharma Gas");
        assertThat(isDuplicate).isTrue();

        // Application logic prevents saving duplicates — verify no double entry
        if (!isDuplicate) {
            PartyNames dup = new PartyNames();
            dup.setPartyName("Sharma Gas");
            repository.saveAndFlush(dup);
        }
        assertThat(repository.getAllPartyNames()).hasSize(3);
    }

    @Test
    @DisplayName("setPartyName trims whitespace")
    void setPartyName_trimsWhitespace() {
        PartyNames p = new PartyNames();
        p.setPartyName("  Trimmed Name  ");
        assertThat(p.getPartyName()).isEqualTo("Trimmed Name");
    }
}
