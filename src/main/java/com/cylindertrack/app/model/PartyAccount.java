package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "party_accounts")
public class PartyAccount {

    @Id
    @NotBlank
    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    /** Stored as a single column; lines separated by \n for multi-line rendering. */
    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "state_name", length = 50)
    private String stateName;

    // ── Getters / setters ──────────────────────────────────────────────────────

    public String getPartyName()  { return partyName; }
    public void setPartyName(String v) { this.partyName = v; }

    /** Raw address string — used by billing / Excel generator. */
    public String getAddress()    { return address; }
    public void setAddress(String v) { this.address = v; }

    public String getPhone()      { return phone; }
    public void setPhone(String v) { this.phone = v; }

    public String getGstin()      { return gstin; }
    public void setGstin(String v) { this.gstin = v != null ? v.toUpperCase().trim() : null; }

    public String getStateCode()  { return stateCode; }
    public void setStateCode(String v) { this.stateCode = v; }

    public String getStateName()  { return stateName; }
    public void setStateName(String v) { this.stateName = v; }

    // ── Virtual address line helpers (used by the form only) ──────────────────

    /** First line of address (everything before the first \n). */
    public String getAddressLine1() {
        if (address == null || address.isBlank()) return "";
        int nl = address.indexOf('\n');
        return nl >= 0 ? address.substring(0, nl).trim() : address.trim();
    }

    /** Second line of address (everything after the first \n). */
    public String getAddressLine2() {
        if (address == null) return "";
        int nl = address.indexOf('\n');
        return nl >= 0 ? address.substring(nl + 1).trim() : "";
    }

    /** Called by form binding — joins lines back into the single address column. */
    public void setAddressLine1(String v) {
        String line2 = getAddressLine2();
        this.address = buildAddress(v != null ? v.trim() : "", line2);
    }

    public void setAddressLine2(String v) {
        String line1 = getAddressLine1();
        this.address = buildAddress(line1, v != null ? v.trim() : "");
    }

    private static String buildAddress(String l1, String l2) {
        if (l1.isBlank() && l2.isBlank()) return null;
        if (l2.isBlank()) return l1;
        if (l1.isBlank()) return l2;
        return l1 + "\n" + l2;
    }
}
