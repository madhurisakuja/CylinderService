package com.cylindertrack.app.controller;

import com.cylindertrack.app.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    @Autowired private BillingService           billingService;
    @Autowired private ExcelBillGenerator       excelGenerator;
    @Autowired private CylinderPriceRepository  priceRepository;
    @Autowired private PartyPriceRepository     partyPriceRepository;
    @Autowired private PartyNamesRepository     partyNamesRepository;
    @Autowired private PartyAccountRepository   partyAccountRepository;
    @Autowired private HsnCodeRepository        hsnCodeRepository;
    @Autowired private InvoiceCounterRepository invoiceCounterRepository;

    private static final String NAV = "billing/generate";

    // ── Shared nav helper ─────────────────────────────────────────────────────

    private void addNav(Model model) {
        model.addAttribute("partyNames", partyNamesRepository.getAllPartyNames());
    }

    // ── Prices ────────────────────────────────────────────────────────────────

    @GetMapping("/prices")
    public String pricesPage(Model model) {
        Map<String, BigDecimal> priceMap = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values())
            priceMap.put(t.name(), new BigDecimal("100.00"));
        priceRepository.findAll().forEach(p -> priceMap.put(p.getGasType(), p.getPrice()));
        model.addAttribute("priceMap",    priceMap);
        model.addAttribute("partyNames",  partyNamesRepository.getAllPartyNames());
        model.addAttribute("partyPrices", partyPriceRepository.findAll());
        model.addAttribute("gasTypes",    Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        return "billing/prices";
    }

    @PostMapping("/prices/default")
    public RedirectView updateDefaultPrice(@RequestParam String gasType,
                                           @RequestParam BigDecimal price,
                                           RedirectAttributes ra) {
        CylinderPrice cp = priceRepository.findByGasType(gasType)
            .orElseGet(() -> { CylinderPrice n = new CylinderPrice(); n.setGasType(gasType); return n; });
        cp.setPrice(price);
        priceRepository.save(cp);
        ra.addFlashAttribute("priceSuccess", "Default price for " + gasType + " updated to ₹" + price);
        return new RedirectView("/billing/prices", true);
    }

    @PostMapping("/prices/party")
    public RedirectView updatePartyPrice(@RequestParam String partyName,
                                         @RequestParam String gasType,
                                         @RequestParam BigDecimal price,
                                         RedirectAttributes ra) {
        PartyPrice pp = partyPriceRepository.findByPartyNameAndGasType(partyName, gasType)
            .orElseGet(() -> { PartyPrice n = new PartyPrice(); n.setPartyName(partyName); n.setGasType(gasType); return n; });
        pp.setPrice(price);
        partyPriceRepository.save(pp);
        ra.addFlashAttribute("priceSuccess", partyName + " / " + gasType + " = ₹" + price);
        return new RedirectView("/billing/prices", true);
    }

    @PostMapping("/prices/party/delete")
    public RedirectView deletePartyPrice(@RequestParam Long id, RedirectAttributes ra) {
        partyPriceRepository.deleteById(id);
        ra.addFlashAttribute("priceSuccess", "Party-specific price removed.");
        return new RedirectView("/billing/prices", true);
    }

    // ── Party accounts ────────────────────────────────────────────────────────

    @GetMapping("/party-accounts")
    public String partyAccountsPage(@RequestParam(required = false) String party,
                                    Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) model.addAttribute("saveSuccess", flash.get("saveSuccess"));
        List<String> allParties = partyNamesRepository.getAllPartyNames();
        String preloaded = (party != null && !party.isBlank()) ? party
            : (allParties.isEmpty() ? "" : allParties.get(allParties.size() - 1));
        PartyAccount existing = partyAccountRepository.findByPartyName(preloaded)
            .orElse(new PartyAccount());
        if (existing.getPartyName() == null) existing.setPartyName(preloaded);
        model.addAttribute("partyAccount", existing);
        model.addAttribute("partyNames",   allParties);
        model.addAttribute("allAccounts",  partyAccountRepository.findAll());
        model.addAttribute("stateCodes",   BillingService.STATE_CODES);
        return "billing/partyAccounts";
    }

    @PostMapping("/party-accounts")
    public RedirectView savePartyAccount(@ModelAttribute PartyAccount pa, RedirectAttributes ra) {
        if (pa.getGstin() != null && !pa.getGstin().isBlank()) {
            if (pa.getStateCode() == null || pa.getStateCode().isBlank())
                pa.setStateCode(BillingService.stateCodeFromGstin(pa.getGstin()));
            if (pa.getStateName() == null || pa.getStateName().isBlank())
                pa.setStateName(BillingService.stateNameFromGstin(pa.getGstin()));
        }
        partyAccountRepository.save(pa);
        ra.addFlashAttribute("saveSuccess", "Details saved for " + pa.getPartyName());
        return new RedirectView("/billing/party-accounts", true);
    }

    // ── HSN codes ─────────────────────────────────────────────────────────────

    @GetMapping("/hsn")
    public String hsnPage(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) model.addAttribute("hsnSuccess", flash.get("hsnSuccess"));
        Map<String, String[]> hsnMap = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values()) hsnMap.put(t.name(), new String[]{"", ""});
        hsnCodeRepository.findAll().forEach(h -> hsnMap.put(h.getGasType(), new String[]{
            h.getHsnCode() != null ? h.getHsnCode() : "",
            h.getDescription() != null ? h.getDescription() : ""
        }));
        model.addAttribute("hsnMap", hsnMap);
        return "billing/hsn";
    }

    @PostMapping("/hsn")
    public RedirectView saveHsn(@RequestParam String gasType,
                                @RequestParam String hsnCode,
                                @RequestParam(required = false) String description,
                                RedirectAttributes ra) {
        HsnCode h = hsnCodeRepository.findByGasType(gasType)
            .orElseGet(() -> { HsnCode n = new HsnCode(); n.setGasType(gasType); return n; });
        h.setHsnCode(hsnCode.trim());
        h.setDescription(description);
        hsnCodeRepository.save(h);
        ra.addFlashAttribute("hsnSuccess", "HSN code for " + gasType + " saved.");
        return new RedirectView("/billing/hsn", true);
    }

    // ── Invoice counter ───────────────────────────────────────────────────────

    @GetMapping("/counter")
    public String counterPage(Model model) {
        InvoiceCounter c = invoiceCounterRepository.findById(1)
            .orElseGet(() -> { InvoiceCounter n = new InvoiceCounter(); return invoiceCounterRepository.save(n); });
        model.addAttribute("counter", c);
        return "billing/counter";
    }

    @PostMapping("/counter")
    public RedirectView updateCounter(@RequestParam Integer startNumber, RedirectAttributes ra) {
        InvoiceCounter c = invoiceCounterRepository.findById(1).orElseGet(InvoiceCounter::new);
        c.setCurrentNumber(startNumber);
        invoiceCounterRepository.save(c);
        ra.addFlashAttribute("counterSuccess", "Invoice counter set to " + startNumber);
        return new RedirectView("/billing/counter", true);
    }

    // ── Bill generation ───────────────────────────────────────────────────────

    @GetMapping("/generate")
    public String billForm(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) model.addAttribute("billError", flash.get("billError"));
        LocalDate now = LocalDate.now();
        model.addAttribute("partyNames",   partyNamesRepository.getAllPartyNames());
        model.addAttribute("currentYear",  now.getYear());
        model.addAttribute("currentMonth", now.getMonthValue());
        return "billing/generate";
    }

    /** Single party bill — download immediately */
    @PostMapping("/generate")
    public void generateBill(@RequestParam String partyName,
                             @RequestParam int year,
                             @RequestParam int month,
                             @RequestParam(defaultValue = "0") BigDecimal discount,
                             @RequestParam(defaultValue = "0") BigDecimal security,
                             @RequestParam(defaultValue = "0") BigDecimal tc,
                             HttpServletResponse response) throws IOException {

        BillSummary bill = billingService.generateBill(partyName, year, month, discount, security, tc);
        if(bill.isEmpty()){
             response.sendError(404, "No entries found for the selected month.");
            return;
        }
        byte[] xlsx = excelGenerator.generate(List.of(bill));
        String filename = sanitize(partyName) + "_" +
                          bill.getInvoiceNumber().replace("/", "-") + ".xlsx";
        writeExcel(response, xlsx, filename);
    }

    /** Bulk — all parties for the month, one sheet each in a single workbook */
    @PostMapping("/generate-all")
    public void generateAllBills(@RequestParam int year,
                                 @RequestParam int month,
                                 @RequestParam(defaultValue = "0") BigDecimal discount,
                                 @RequestParam(defaultValue = "0") BigDecimal security,
                                 @RequestParam(defaultValue = "0") BigDecimal tc,
                                 HttpServletResponse response) throws IOException {

        List<BillSummary> bills = billingService.generateAllBills(year, month, discount, security, tc);
        if (bills.isEmpty()) {
            response.sendError(404, "No entries found for the selected month.");
            return;
        }
        byte[] xlsx = excelGenerator.generate(bills);
        String monthStr = String.format("%02d", month);
        String filename = "ALL_BILLS_" + year + "_" + monthStr + ".xlsx";
        writeExcel(response, xlsx, filename);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void writeExcel(HttpServletResponse response, byte[] xlsx, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(xlsx);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
