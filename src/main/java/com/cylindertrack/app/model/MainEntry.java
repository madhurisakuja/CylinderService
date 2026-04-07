package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.DateFormat;
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

    public MainEntry() {}

    public List<String> getCylinderTypes() {
        List<String> list = new ArrayList<>();
        for (CylinderTypeF value : CylinderTypeF.values()) list.add(value.name());
        return list;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public Integer getCfull() { return cfull != null ? cfull : 0; }
    public void setCfull(Integer cfull) { this.cfull = cfull; }
    public Integer getCempty() { return cempty != null ? cempty : 0; }
    public void setCempty(Integer cempty) { this.cempty = cempty; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public Integer getCholding() { return cholding; }
    public void setCholding(Integer cholding) { this.cholding = cholding; }
    public String getCtype() { return ctype; }
    public void setCtype(String ctype) { this.ctype = ctype; }
    public Boolean getIsPurchase() { return isPurchase; }
    public void setIsPurchase(Boolean isPurchase) { this.isPurchase = isPurchase; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Integer getMonthValue() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return LocalDate.parse(df.format(date)).getMonthValue();
    }

    public String getStringDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    @Override
    public String toString() {
        return "MainEntry{party=" + partyName + ", type=" + ctype +
               ", full=" + cfull + ", empty=" + cempty +
               ", holding=" + cholding + ", date=" + date + "}";
    }
}
