package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

public class BillSummary {
    private String partyName;
    private PartyAccount partyAccount;
    private Date invoiceDate;          // last day of selected month
    private String invoiceNumber;      // e.g. 584/2025-26
    private String fiscalYear;         // e.g. 2025-26
    private List<BillLineItem> lineItems;
    private BigDecimal subtotal;
    private BigDecimal cgst;           // 9%
    private BigDecimal sgst;           // 9%
    private BigDecimal totalWithGst;
    private BigDecimal securityDeposit; // deducted
    private BigDecimal tcCharge;        // added (transport/labour)
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private String amountInWords;

    public BillSummary(String partyName, PartyAccount partyAccount,
                       Date invoiceDate, String invoiceNumber, String fiscalYear,
                       List<BillLineItem> lineItems,
                       BigDecimal discountAmount, BigDecimal securityDeposit,
                       BigDecimal tcCharge) {
        this.partyName       = partyName;
        this.partyAccount    = partyAccount;
        this.invoiceDate     = invoiceDate;
        this.invoiceNumber   = invoiceNumber;
        this.fiscalYear      = fiscalYear;
        this.lineItems       = lineItems;
        this.discountAmount  = nvl(discountAmount);
        this.securityDeposit = nvl(securityDeposit);
        this.tcCharge        = nvl(tcCharge);

        this.subtotal     = lineItems.stream().map(BillLineItem::getTaxableAmount)
                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.cgst         = this.subtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        this.sgst         = this.cgst;
        this.totalWithGst = this.subtotal.add(this.cgst).add(this.sgst);
        this.grandTotal   = this.totalWithGst
                                .add(this.tcCharge)
                                .subtract(this.securityDeposit)
                                .subtract(this.discountAmount)
                                .max(BigDecimal.ZERO);
        this.amountInWords = AmountInWords.convert(this.grandTotal);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    public String getPartyName()         { return partyName; }
    public PartyAccount getPartyAccount(){ return partyAccount; }
    public Date getInvoiceDate()         { return invoiceDate; }
    public String getInvoiceNumber()     { return invoiceNumber; }
    public String getFiscalYear()        { return fiscalYear; }
    public List<BillLineItem> getLineItems() { return lineItems; }
    public BigDecimal getSubtotal()      { return subtotal; }
    public BigDecimal getCgst()          { return cgst; }
    public BigDecimal getSgst()          { return sgst; }
    public BigDecimal getTotalWithGst()  { return totalWithGst; }
    public BigDecimal getSecurityDeposit(){ return securityDeposit; }
    public BigDecimal getTcCharge()      { return tcCharge; }
    public BigDecimal getDiscountAmount(){ return discountAmount; }
    public BigDecimal getGrandTotal()    { return grandTotal; }
    public String getAmountInWords()     { return amountInWords; }
    public boolean hasDiscount()         { return discountAmount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasSecurity()         { return securityDeposit.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasTc()               { return tcCharge.compareTo(BigDecimal.ZERO) > 0; }
}
