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

    /**
     * Name to print on the bill. If blank, falls back to partyName.
     * Allows e.g. internal name "SHARMA GAS" to print as "M/S SHARMA GAS AGENCY".
     */
    @Column(name = "billing_name", length = 200)
    private String billingName;

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

    // ── Getters / setters ─────────────────────────────────────────────────────

    public String getPartyName()  { return partyName; }
    public void setPartyName(String v) { this.partyName = v; }

    public String getBillingName() { return billingName; }
    public void setBillingName(String v) {
        this.billingName = (v != null && !v.isBlank()) ? v.trim() : null;
    }

    /** Returns billingName if set, otherwise partyName — used by Excel generator. */
    public String getEffectiveBillingName() {
        return (billingName != null && !billingName.isBlank()) ? billingName : partyName;
    }

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

    // ── Virtual address line helpers ──────────────────────────────────────────

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
        this.address = buildAddress(v != null ? v.trim() : "", getAddressLine2());
    }

    public void setAddressLine2(String v) {
        this.address = buildAddress(getAddressLine1(), v != null ? v.trim() : "");
    }

    private static String buildAddress(String l1, String l2) {
        if (l1.isBlank() && l2.isBlank()) return null;
        if (l2.isBlank()) return l1;
        if (l1.isBlank()) return l2;
        return l1 + "\n" + l2;
    }
}
