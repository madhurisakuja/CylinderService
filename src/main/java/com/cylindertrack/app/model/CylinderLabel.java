package com.cylindertrack.app.model;

import jakarta.persistence.*;

/**
 * Stores the "Particulars" label for a gas type on a bill.
 * If partyName is null → default label for all parties.
 * If partyName is set → overrides the default for that party.
 */
@Entity
@Table(name = "cylinder_labels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"gas_type", "party_name"}))
public class CylinderLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LABEL_SEQ")
    @SequenceGenerator(name = "LABEL_SEQ", sequenceName = "LABEL_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "gas_type", nullable = false, length = 20)
    private String gasType;

    /** Null = default label. Set = party-specific override. */
    @Column(name = "party_name", length = 100)
    private String partyName;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    public Long getId() { return id; }
    public String getGasType() { return gasType; }
    public void setGasType(String v) { this.gasType = v; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String v) { this.partyName = (v != null && !v.isBlank()) ? v.trim() : null; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public boolean isDefault() { return partyName == null || partyName.isBlank(); }
}
