package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

public class BillSummary {
    private String partyName;
    private PartyAccount partyAccount;
    private String partyUom;          // NOS / M3 / blank from PartyNames
    private Date invoiceDate;
    private String invoiceNumber;
    private String fiscalYear;
    private List<BillLineItem> lineItems;
    private BigDecimal subtotal;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal totalWithGst;
    private BigDecimal securityDeposit;
    private BigDecimal tcCharge;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private String amountInWords;

    public BillSummary(String partyName, PartyAccount partyAccount, String partyUom,
                       Date invoiceDate, String invoiceNumber, String fiscalYear,
                       List<BillLineItem> lineItems,
                       BigDecimal discountAmount, BigDecimal securityDeposit, BigDecimal tcCharge) {
        this.partyName       = partyName;
        this.partyAccount    = partyAccount;
        this.partyUom        = partyUom != null ? partyUom : "";
        this.invoiceDate     = invoiceDate;
        this.invoiceNumber   = invoiceNumber;
        this.fiscalYear      = fiscalYear;
        this.lineItems       = lineItems;
        this.discountAmount  = nvl(discountAmount);
        this.securityDeposit = nvl(securityDeposit);
        this.tcCharge        = nvl(tcCharge);

        this.subtotal     = lineItems.stream().map(BillLineItem::getTaxableAmount)
                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.cgst         = subtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        this.sgst         = cgst;
        this.totalWithGst = subtotal.add(cgst).add(sgst);
        this.grandTotal   = totalWithGst.add(this.tcCharge)
                                        .subtract(this.securityDeposit)
                                        .subtract(this.discountAmount)
                                        .max(BigDecimal.ZERO);
        this.amountInWords = AmountInWords.convert(grandTotal);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    public String getPartyName()          { return partyName; }
    public PartyAccount getPartyAccount() { return partyAccount; }
    public String getPartyUom()           { return partyUom; }
    public boolean hasUom()               { return partyUom != null && !partyUom.isBlank(); }
    public Date getInvoiceDate()          { return invoiceDate; }
    public String getInvoiceNumber()      { return invoiceNumber; }
    public String getFiscalYear()         { return fiscalYear; }
    public List<BillLineItem> getLineItems() { return lineItems; }
    public BigDecimal getSubtotal()       { return subtotal; }
    public BigDecimal getCgst()           { return cgst; }
    public BigDecimal getSgst()           { return sgst; }
    public BigDecimal getTotalWithGst()   { return totalWithGst; }
    public BigDecimal getSecurityDeposit(){ return securityDeposit; }
    public BigDecimal getTcCharge()       { return tcCharge; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getGrandTotal()     { return grandTotal; }
    public String getAmountInWords()      { return amountInWords; }
    public boolean hasDiscount()          { return discountAmount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasSecurity()          { return securityDeposit.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasTc()                { return tcCharge.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isEmpty()              { return lineItems == null || lineItems.isEmpty(); }
}
