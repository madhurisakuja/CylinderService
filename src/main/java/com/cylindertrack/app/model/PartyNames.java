package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "party_names")
public class PartyNames {

    @Id
    @NotBlank(message = "Party name must not be blank")
    @Size(max = 100, message = "Party name must be 100 characters or fewer")
    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) {
        this.partyName = partyName != null ? partyName.trim() : null;
    }
}
