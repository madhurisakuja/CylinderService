package com.cylindertrack.app.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public static final Map<String, String> STATE_CODES = new LinkedHashMap<>();
    static {
        STATE_CODES.put("01","Jammu & Kashmir"); STATE_CODES.put("02","Himachal Pradesh");
        STATE_CODES.put("03","Punjab");          STATE_CODES.put("04","Chandigarh");
        STATE_CODES.put("05","Uttarakhand");     STATE_CODES.put("06","Haryana");
        STATE_CODES.put("07","Delhi");           STATE_CODES.put("08","Rajasthan");
        STATE_CODES.put("09","Uttar Pradesh");   STATE_CODES.put("10","Bihar");
        STATE_CODES.put("19","West Bengal");     STATE_CODES.put("20","Jharkhand");
        STATE_CODES.put("21","Odisha");          STATE_CODES.put("22","Chhattisgarh");
        STATE_CODES.put("23","Madhya Pradesh");  STATE_CODES.put("24","Gujarat");
        STATE_CODES.put("27","Maharashtra");     STATE_CODES.put("29","Karnataka");
        STATE_CODES.put("32","Kerala");          STATE_CODES.put("33","Tamil Nadu");
        STATE_CODES.put("36","Telangana");       STATE_CODES.put("37","Andhra Pradesh");
    }

    public static String stateNameFromGstin(String g) {
        if (g==null||g.trim().length()<2) return null;
        return STATE_CODES.get(g.trim().substring(0,2));
    }
    public static String stateCodeFromGstin(String g) {
        if (g==null||g.trim().length()<2) return null;
        return g.trim().substring(0,2);
    }

    @Autowired private BillingRepository        billingRepository;
    @Autowired private CylinderPriceRepository  priceRepository;
    @Autowired private PartyPriceRepository     partyPriceRepository;
    @Autowired private PartyAccountRepository   partyAccountRepository;
    @Autowired private PartyNamesRepository     partyNamesRepository;
    @Autowired private HsnCodeRepository        hsnCodeRepository;
    @Autowired private CylinderLabelRepository  labelRepository;
    @Autowired private InvoiceCounterRepository invoiceCounterRepository;

    /** Single party bill with explicit date range */
    @Transactional
    public BillSummary generateBill(String partyName, Date fromDate, Date toDate,
                                    BigDecimal discount, BigDecimal security, BigDecimal tc) {
        Date invoiceDate = Date.from(LocalDate.now(IST).atStartOfDay(IST).toInstant());
        LocalDate from = fromDate.toInstant().atZone(IST).toLocalDate();
        return buildBill(partyName, from.getYear(), from.getMonthValue(),
                         fromDate, toDate, invoiceDate, discount, security, tc);
    }

    /** Bulk — all active parties for a date range. No per-party adjustments. */
    // NOT @Transactional — each buildBill runs its own transaction so counter increments are visible
    public List<BillSummary> generateAllBills(Date fromDate, Date toDate) {
        Date invoiceDate = Date.from(LocalDate.now(IST).atStartOfDay(IST).toInstant());
        List<String> parties = billingRepository.findPartiesWithActivityInMonth(fromDate, toDate);
        List<BillSummary> bills = new ArrayList<>();
        LocalDate from = fromDate.toInstant().atZone(IST).toLocalDate();
        for (String party : parties) {
            bills.add(buildBill(party, from.getYear(), from.getMonthValue(),
                                fromDate, toDate, invoiceDate,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        log.info("Bulk bills: {} parties", bills.size());
        return bills;
    }

    /**
     * Builds a blank template bill for a party — no qty/amounts, just structure.
     * Used for custom/instant bill generation without data entries.
     */
    @Transactional
    public BillSummary buildCustomTemplate(String partyName) {
        Date invoiceDate = Date.from(LocalDate.now(IST).atStartOfDay(IST).toInstant());
        LocalDate now = LocalDate.now(IST);
        int year = now.getYear(); int month = now.getMonthValue();
        int fiscalStart = (month >= 4) ? year : year - 1;

        String partyUom = partyNamesRepository.findUomByPartyName(partyName);

        // Batch-fetch all HSN codes and labels in 2 queries instead of 7+7
        Map<String, String> hsnMap = new HashMap<>();
        hsnCodeRepository.findAll().forEach(h -> hsnMap.put(h.getGasType(), h.getHsnCode() != null ? h.getHsnCode() : ""));
        Map<String, String> defaultLabelMap = new HashMap<>();
        labelRepository.findAllDefaults().forEach(l -> defaultLabelMap.put(l.getGasType(), l.getLabel()));
        Map<String, String> partyLabelMap = new HashMap<>();
        labelRepository.findAllOverrides().stream()
            .filter(l -> partyName.equals(l.getPartyName()))
            .forEach(l -> partyLabelMap.put(l.getGasType(), l.getLabel()));

        List<BillLineItem> lineItems = new ArrayList<>();
        int sl = 1;
        for (CylinderTypeF t : CylinderTypeF.values()) {
            String gasType = t.name();
            String label = partyLabelMap.getOrDefault(gasType,
                           defaultLabelMap.getOrDefault(gasType, gasType));
            String hsn   = hsnMap.getOrDefault(gasType, "");
            lineItems.add(new BillLineItem(sl++, label, hsn, partyUom, 0, BigDecimal.ZERO));
        }

        // Use real invoice counter — same as a regular bill
        InvoiceCounter counter = invoiceCounterRepository.findById(1)
            .orElseGet(() -> {
                InvoiceCounter c = new InvoiceCounter();
                c.setFiscalStartYear(fiscalStart);
                return invoiceCounterRepository.save(c);
            });
        if (!counter.getFiscalStartYear().equals(fiscalStart)) {
            invoiceCounterRepository.resetForNewFiscalYear(fiscalStart);
            counter.setCurrentNumber(1);
            counter.setFiscalStartYear(fiscalStart);
        }
        int invoiceNum = counter.getCurrentNumber();
        invoiceCounterRepository.increment();

        String fiscalYear    = fiscalStart + "-" + String.valueOf(fiscalStart + 1).substring(2);
        String invoiceNumber = String.format("%02d", invoiceNum) + "/" + fiscalYear;

        PartyAccount account = resolveAccount(partyName);
        log.info("Custom template: invoice={} party={}", invoiceNumber, partyName);
        return new BillSummary(partyName, account, partyUom, invoiceDate, invoiceNumber,
                               fiscalYear, lineItems, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional
    public BillSummary buildBill(String partyName, int year, int month,
                                 Date fromDate, Date toDate, Date invoiceDate,
                                 BigDecimal discountAmount, BigDecimal securityDeposit, BigDecimal tcCharge) {

        List<Object[]> rows = billingRepository.findBillableEntriesGrouped(partyName, fromDate, toDate);
        String partyUom = partyNamesRepository.findUomByPartyName(partyName);

        List<BillLineItem> lineItems = new ArrayList<>();
        int sl = 1;
        for (Object[] row : rows) {
            String gasType  = (String) row[0];
            int qty         = ((Number) row[1]).intValue();
            BigDecimal rate = resolvePrice(partyName, gasType);
            String hsn      = hsnCodeRepository.findByGasType(gasType).map(HsnCode::getHsnCode).orElse("");
            String label    = resolveLabel(partyName, gasType);
            lineItems.add(new BillLineItem(sl++, label, hsn, partyUom, qty, rate));
        }

        int fiscalStart = (month >= 4) ? year : year - 1;
        InvoiceCounter counter = invoiceCounterRepository.findById(1)
            .orElseGet(() -> {
                InvoiceCounter c = new InvoiceCounter();
                c.setFiscalStartYear(fiscalStart);
                return invoiceCounterRepository.save(c);
            });

        if (!counter.getFiscalStartYear().equals(fiscalStart)) {
            invoiceCounterRepository.resetForNewFiscalYear(fiscalStart);
            counter.setCurrentNumber(1);
            counter.setFiscalStartYear(fiscalStart);
        }

        int invoiceNum = counter.getCurrentNumber();
        invoiceCounterRepository.increment();

        String fiscalYear    = fiscalStart + "-" + String.valueOf(fiscalStart+1).substring(2);
        String invoiceNumber = String.format("%02d", invoiceNum) + "/" + fiscalYear;

        PartyAccount account = resolveAccount(partyName);
        log.info("Bill: invoice={} party={} items={}", invoiceNumber, partyName, lineItems.size());
        return new BillSummary(partyName, account, partyUom, invoiceDate, invoiceNumber,
                               fiscalYear, lineItems, discountAmount, securityDeposit, tcCharge);
    }

    private PartyAccount resolveAccount(String partyName) {
        PartyAccount account = partyAccountRepository.findByPartyName(partyName).orElse(null);
        if (account != null && account.getGstin() != null && !account.getGstin().isBlank()) {
            if (account.getStateCode()==null||account.getStateCode().isBlank())
                account.setStateCode(stateCodeFromGstin(account.getGstin()));
            if (account.getStateName()==null||account.getStateName().isBlank())
                account.setStateName(stateNameFromGstin(account.getGstin()));
        }
        return account;
    }

    public String resolveLabel(String partyName, String gasType) {
        return labelRepository.findByGasTypeAndPartyName(gasType, partyName)
            .map(CylinderLabel::getLabel)
            .orElseGet(() -> labelRepository.findDefaultByGasType(gasType)
                .map(CylinderLabel::getLabel)
                .orElse(gasType));
    }

    public Date[] monthRange(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last  = first.withDayOfMonth(first.lengthOfMonth());
        return new Date[]{
            Date.from(first.atStartOfDay(IST).toInstant()),
            Date.from(last.atTime(23,59,59).atZone(IST).toInstant())
        };
    }

    public BigDecimal resolvePrice(String partyName, String gasType) {
        return partyPriceRepository.findByPartyNameAndGasType(partyName, gasType)
            .map(PartyPrice::getPrice)
            .orElseGet(() -> priceRepository.findByGasType(gasType)
                .map(CylinderPrice::getPrice)
                .orElse(DEFAULT_PRICE));
    }

    public void seedDefaultPricesIfEmpty() {
        if (priceRepository.count() == 0) {
            for (CylinderTypeF t : CylinderTypeF.values()) {
                CylinderPrice p = new CylinderPrice(); p.setGasType(t.name()); p.setPrice(DEFAULT_PRICE);
                priceRepository.save(p);
            }
        }
        if (invoiceCounterRepository.count() == 0) {
            InvoiceCounter c = new InvoiceCounter();
            LocalDate now = LocalDate.now(IST);
            c.setFiscalStartYear(now.getMonthValue()>=4 ? now.getYear() : now.getYear()-1);
            invoiceCounterRepository.save(c);
        }
        seedHsnCodesIfEmpty();
        seedDefaultLabelsIfEmpty();
    }

    private void seedHsnCodesIfEmpty() {
        if (hsnCodeRepository.count() == 0) {
            Map<String,String[]> hsn = new LinkedHashMap<>();
            hsn.put("OXY",new String[]{"28044090","OXYGEN"}); hsn.put("LPG",new String[]{"27111900","LPG 19KG"});
            hsn.put("DA",new String[]{"29012910","DA Gas"});   hsn.put("ARGON",new String[]{"997319","SAC"});
            hsn.put("NITROGEN",new String[]{"28043000","NITROGEN"}); hsn.put("ARGOSHIELD",new String[]{"28042100","ARGOSHIELD"});
            hsn.put("CO2",new String[]{"28112190","CO2"});
            hsn.forEach((type,vals) -> { HsnCode h=new HsnCode(); h.setGasType(type); h.setHsnCode(vals[0]); h.setDescription(vals[1]); hsnCodeRepository.save(h); });
        }
    }

    private void seedDefaultLabelsIfEmpty() {
        if (labelRepository.count() == 0) {
            Map<String,String> d = new LinkedHashMap<>();
            d.put("OXY","OXYGEN"); d.put("CO2","CARBON DIOXIDE"); d.put("LPG","LPG 19KG");
            d.put("DA","DISSOLVED ACETYLENE"); d.put("ARGON","ARGON");
            d.put("ARGOSHIELD","ARGOSHIELD"); d.put("NITROGEN","NITROGEN");
            d.forEach((type,label) -> {
                CylinderLabel l = new CylinderLabel(); l.setGasType(type); l.setLabel(label);
                labelRepository.save(l);
            });
        }
    }
}
