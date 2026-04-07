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

    public String getPartyName() { return partyName; }
    public void setPartyName(String v) { this.partyName = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getGstin() { return gstin; }
    public void setGstin(String v) { this.gstin = v != null ? v.toUpperCase().trim() : null; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String v) { this.stateCode = v; }
    public String getStateName() { return stateName; }
    public void setStateName(String v) { this.stateName = v; }
}
