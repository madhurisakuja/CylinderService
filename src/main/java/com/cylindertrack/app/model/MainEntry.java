package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "main_entry")
public class MainEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MAIN_ENTRY_SEQ")
    @SequenceGenerator(name = "MAIN_ENTRY_SEQ", sequenceName = "MAIN_ENTRY_SEQ", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "party_name", nullable = false)
    private String partyName;

    @Column(name = "cfull")
    private Integer cfull = 0;

    @Column(name = "cempty")
    private Integer cempty = 0;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "entry_date", nullable = false)
    private Date date;

    @Column(name = "cholding")
    private Integer cholding;

    @NotBlank
    @Column(name = "ctype", nullable = false)
    private String ctype;

    @Column(name = "is_purchase")
    private Boolean isPurchase = false;

    @Column(name = "remarks")
    private String remarks;

    /** Set by DB on INSERT — used for daily report. Hibernate ddl-auto=update adds this column. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    public MainEntry() {}

    public List<String> getCylinderTypes() {
        List<String> list = new ArrayList<>();
        for (CylinderTypeF value : CylinderTypeF.values()) list.add(value.name());
        return list;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String v) { this.partyName = v; }
    public Integer getCfull() { return cfull != null ? cfull : 0; }
    public void setCfull(Integer v) { this.cfull = v; }
    public Integer getCempty() { return cempty != null ? cempty : 0; }
    public void setCempty(Integer v) { this.cempty = v; }
    public Date getDate() { return date; }
    public void setDate(Date v) { this.date = v; }
    public Integer getCholding() { return cholding; }
    public void setCholding(Integer v) { this.cholding = v; }
    public String getCtype() { return ctype; }
    public void setCtype(String v) { this.ctype = v; }
    public Boolean getIsPurchase() { return isPurchase; }
    public void setIsPurchase(Boolean v) { this.isPurchase = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Date getCreatedAt() { return createdAt; }

    public Integer getMonthValue() {
        return LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(date)).getMonthValue();
    }
    public String getStringDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    @Override
    public String toString() {
        return "MainEntry{party=" + partyName + ",type=" + ctype +
               ",full=" + cfull + ",empty=" + cempty + ",date=" + date + "}";
    }
}
