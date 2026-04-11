package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "party_names")
public class PartyNames {

    @Id
    @NotBlank
    @Size(max = 100)
    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    /** Optional UOM for billing: NOS, M3, or null/blank = no UOM column on bill */
    @Column(name = "uom_type", length = 10)
    private String uomType;

    public String getPartyName() { return partyName; }
    public void setPartyName(String v) { this.partyName = v != null ? v.trim() : null; }
    public String getUomType() { return uomType; }
    public void setUomType(String v) { this.uomType = (v != null && !v.isBlank()) ? v.trim() : null; }
    public boolean hasUom() { return uomType != null && !uomType.isBlank(); }
}
