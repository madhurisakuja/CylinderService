package com.cylindertrack.app.controller;

import com.cylindertrack.app.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired private MainEntryRepository    mainEntryRepo;
    @Autowired private NewCylinderFService    cylinderRepo;
    @Autowired private PartyNamesRepository   partyNamesRepo;
    @Autowired private EntryService           entryService;

    // ── Login / Home ──────────────────────────────────────────────────────────

    // The catch-all mapping
    @GetMapping("/**")
    public String handleUnknownPaths() {
        return "redirect:/myhome";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping({"/", "/newhome", "/myhome"})
    public String home(Model model) {
        return "newhome";
    }

    // ── New Party ─────────────────────────────────────────────────────────────

    @GetMapping("/newPartyEntryF")
    public String newPartyGet(Model model) {
        model.addAttribute("entryParty", new PartyNames());
        model.addAttribute("allParties", partyNamesRepo.findAll());  // full objects for UOM display
        return "newPartyEntryF";
    }
    @PostMapping("/newPartyEntryF")
    public RedirectView newPartyPost(@ModelAttribute("entryParty") PartyNames partyName,
                                     RedirectAttributes ra) {
        List<String> existing = partyNamesRepo.getAllPartyNamesAll();
        boolean isNew = !existing.contains(partyName.getPartyName());
        if (isNew) partyNamesRepo.saveAndFlush(partyName);
        ra.addFlashAttribute("entrySuccess", isNew);
        ra.addFlashAttribute("partyName", partyName.getPartyName());
        return new RedirectView("/newPartyEntryF", true);
    }

    // ── New Bulk Sales Entry ───────────────────────────────────────────────────

    @GetMapping("/newEntryF")
    public String newEntryGet(Model model) {
        Date defDate = mainEntryRepo.getLatestEntryDate();
        LocalDate today = LocalDate.now();
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        
        model.addAttribute("defaultDate", defDate != null
            ? new SimpleDateFormat("yyyy-MM-dd").format(defDate) : new SimpleDateFormat("yyyy-MM-dd").format(todayDate));
        model.addAttribute("partyNames",  partyNamesRepo.getAllPartyNames());
        model.addAttribute("gasTypes",    Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        model.addAttribute("statuses",    Arrays.stream(CylinderStatus.values()).map(Enum::name).toList());
        return "newEntryF";
    }  

    /**
     * Accepts a multi-type bulk entry posted as JSON from the page's JS.
     * Each row = {partyName, date, ctype, cfull, cempty, remarks}.
     */
    @PostMapping("/newEntryF")
    @ResponseBody
    public Map<String, Object> newEntryPost(@RequestBody List<Map<String, String>> rows) {
        List<String> saved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map<String, String> row : rows) {
            try {
                MainEntry e = new MainEntry();
                e.setPartyName(row.get("partyName"));
                e.setCtype(row.get("ctype"));
                e.setCfull(parseInt(row.get("cfull"), 0));
                e.setCempty(parseInt(row.get("cempty"), 0));
                e.setDate(parseDate(row.get("date")));
                e.setRemarks(row.getOrDefault("remarks", ""));

                MainEntry result = entryService.saveSalesEntry(e);
                if (result == null) {
                    Date last = mainEntryRepo.getLastDateEntry(e.getPartyName(), e.getCtype());
                    errors.add("Back-date rejected for " + e.getCtype() + " — last entry was " +
                               new SimpleDateFormat("dd-MM-yyyy").format(last));
                } else {
                    saved.add(e.getCtype() + " (holding=" + result.getCholding() + ")");
                }
            } catch (Exception ex) {
                errors.add("Error processing row: " + ex.getMessage());
                log.error("Entry save error", ex);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("saved", saved);
        resp.put("errors", errors);
        return resp;
    }

    // ── Purchase Entry ────────────────────────────────────────────────────────

    @GetMapping("/purchaseEntryF")
    public String purchaseEntryGet(Model model) {
        Date defDate = mainEntryRepo.getLatestEntryDate();
        model.addAttribute("defaultDate", defDate != null
            ? new SimpleDateFormat("yyyy-MM-dd").format(defDate) : "");
        model.addAttribute("partyNames", partyNamesRepo.getAllPartyNamesPurchaser());
        model.addAttribute("gasTypes",   Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        return "purchaseEntryF";
    }

    @PostMapping("/purchaseEntryF")
    @ResponseBody
    public Map<String, Object> purchaseEntryPost(@RequestBody List<Map<String, String>> rows) {
        List<String> saved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map<String, String> row : rows) {
            try {
                MainEntry e = new MainEntry();
                e.setPartyName(row.get("partyName"));
                e.setCtype(row.get("ctype"));
                e.setCfull(parseInt(row.get("cfull"), 0));
                e.setCempty(parseInt(row.get("cempty"), 0));
                e.setDate(parseDate(row.get("date")));
                e.setRemarks(row.getOrDefault("remarks", ""));

                MainEntry result = entryService.savePurchaseEntry(e);
                if (result == null) {
                    Date last = mainEntryRepo.getLastDateEntry(e.getPartyName(), e.getCtype());
                    errors.add("Back-date rejected for " + e.getCtype() + " — last entry was " +
                               new SimpleDateFormat("dd-MM-yyyy").format(last));
                } else {
                    saved.add(e.getCtype() + " (holding=" + result.getCholding() + ")");
                }
            } catch (Exception ex) {
                errors.add("Error: " + ex.getMessage());
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("saved", saved);
        resp.put("errors", errors);
        return resp;
    }

    // ── Delete Entry ──────────────────────────────────────────────────────────

    @GetMapping("/deleteEntryF")
    public String deleteEntryGet(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) {
            model.addAttribute("deleteSuccess", flash.get("deleteSuccess"));
            model.addAttribute("cylinders",     flash.get("cylinders"));
        }
        Date defDate = mainEntryRepo.getLatestEntryDate();
        MainEntry entry = new MainEntry();
        if (defDate != null) entry.setDate(defDate);
        List<String> all = new ArrayList<>(partyNamesRepo.getAllPartyNamesPurchaser());
        all.addAll(partyNamesRepo.getAllPartyNames());
        model.addAttribute("partyNames", all);
        model.addAttribute("entry", entry);
        return "deleteEntryF";
    }

    /** AJAX: load cylinder numbers for a party+date so the delete page can show a dropdown */
    @GetMapping("/deleteEntryF/cylinders")
    @ResponseBody
    public List<Long> getCylindersForDelete(
            @RequestParam String partyName,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        return cylinderRepo.findCylinderNumbersByPartyAndDate(partyName, date);
    }

    @PostMapping("/deleteEntryF")
    public RedirectView deleteEntryPost(
            @RequestParam String partyName,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam(required = false) Long cylinderNo,
            RedirectAttributes ra) {

        if (cylinderNo != null) {
            // Delete only the specific cylinder entry
            cylinderRepo.deleteByPartyDateAndCylinder(partyName, date, cylinderNo);
            log.info("Deleted cylinder entry: party={} date={} cylinder={}", partyName, date, cylinderNo);
        } else {
            // Delete all entries for party+date (both main_entry and cylinder_entries)
            mainEntryRepo.deleteByPartyAndDate(partyName, date);
            cylinderRepo.deleteByPartyAndDate(partyName, date);
            log.info("Deleted all entries for party={} date={}", partyName, date);
        }
        ra.addFlashAttribute("deleteSuccess", true);
        return new RedirectView("/deleteEntryF", true);
    }

    // ── Cylinder Numbers Tab ──────────────────────────────────────────────────

    @GetMapping("/cylinderNumbers")
    public String cylinderNumbersGet(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) {
            model.addAttribute("saveSuccess",  flash.get("saveSuccess"));
            model.addAttribute("bulkEntries",  flash.get("bulkEntries"));
            model.addAttribute("filterParty",  flash.get("filterParty"));
            model.addAttribute("filterDate",   flash.get("filterDate"));
        }
        List<String> all = new ArrayList<>(partyNamesRepo.getAllPartyNames());
        all.addAll(partyNamesRepo.getAllPartyNamesPurchaser());
        model.addAttribute("partyNames", all);
        model.addAttribute("gasTypes", Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        Date defDate = mainEntryRepo.getLatestEntryDate();
        model.addAttribute("defaultDate", defDate != null
            ? new SimpleDateFormat("yyyy-MM-dd").format(defDate) : "");
        return "cylinderNumbers";
    }

    /**
     * Loads bulk entries for party+date.
     * Returns:
     *   entries      — bulk MainEntry rows (ctype, cfull, cempty)
     *   fullAtParty  — cylinders currently FULL at this party per gas type (for EMPTY dropdown)
     *   savedFull    — already-saved cylinder numbers for FULL per ctype (for preloading)
     *   savedEmpty   — already-saved cylinder numbers for EMPTY per ctype (for preloading)
     */
    @GetMapping("/cylinderNumbers/load")
    @ResponseBody
    public Map<String, Object> loadCylinderNumbersForm(
            @RequestParam String partyName,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {

        List<MainEntry> bulkEntries = mainEntryRepo.findByPartyAndDate(partyName, date);

        // Cylinders FULL at this party per type — candidates for EMPTY return dropdown
        Map<String, List<Long>> fullAtParty = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values()) {
            List<Long> cyls = entryService.getCylindersFullAtParty(partyName, t.name());
            if (!cyls.isEmpty()) fullAtParty.put(t.name(), cyls);
        }

        // Already-saved cylinders for this party+date — preload into inputs
        Map<String, List<Long>> savedFull  = new LinkedHashMap<>();
        Map<String, List<Long>> savedEmpty = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values()) {
            List<Long> sf = cylinderRepo.findSavedCylinders(partyName, date, t.name(), "FULL");
            List<Long> se = cylinderRepo.findSavedCylinders(partyName, date, t.name(), "EMPTY");
            if (!sf.isEmpty()) savedFull.put(t.name(),  sf);
            if (!se.isEmpty()) savedEmpty.put(t.name(), se);
        }

        // Serialise bulk entries as simple maps
        List<Map<String, Object>> entries = new ArrayList<>();
        for (MainEntry e : bulkEntries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        e.getId());
            m.put("ctype",     e.getCtype());
            m.put("cfull",     e.getCfull());
            m.put("cempty",    e.getCempty());
            m.put("date",      new SimpleDateFormat("yyyy-MM-dd").format(e.getDate()));
            entries.add(m);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("entries",     entries);
        resp.put("fullAtParty", fullAtParty);
        resp.put("savedFull",   savedFull);
        resp.put("savedEmpty",  savedEmpty);
        return resp;
    }

    /**
     * Save / update cylinder number entries.
     * Uses upsert pattern: for each ctype+fullType combination present in the submitted rows,
     * delete all existing entries for that combination first, then insert the new ones.
     * This means reloading and re-submitting a partial set correctly replaces prior entries.
     * Blank cylinder numbers in the batch are silently skipped.
     */
    @PostMapping("/cylinderNumbers/save")
    @ResponseBody
    @jakarta.transaction.Transactional
    public Map<String, Object> saveCylinderNumbers(@RequestBody List<Map<String, String>> rows) {
        int saved = 0;
        List<String> warnings = new ArrayList<>();
        Set<Long> seenInBatch  = new LinkedHashSet<>();  // duplicate-within-batch check
        Set<String> deletedSlots = new LinkedHashSet<>(); // track which ctype+status slots were cleared

        // First pass: validate and collect valid rows
        List<Map<String, String>> validRows = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String rawNo = row.get("cylinderNo");
            if (rawNo == null || rawNo.isBlank()) continue;
            try {
                long n = Long.parseLong(rawNo.trim());
                if (n <= 0) continue;
                row.put("cylinderNo", String.valueOf(n));
                validRows.add(row);
            } catch (NumberFormatException e) {
                warnings.add("Skipped non-numeric value: " + rawNo);
            }
        }

        // Second pass: upsert
        for (Map<String, String> row : validRows) {
            try {
                Long cylinderNo  = Long.parseLong(row.get("cylinderNo"));
                String partyName = row.get("partyName");
                String ctype     = row.get("ctype");
                Date   date      = parseDate(row.get("date"));
                String status    = row.get("status"); // FULL or EMPTY

                // Clear existing entries for this ctype+status once per slot group
                String slotKey = partyName + "|" + row.get("date") + "|" + ctype + "|" + status;
                if (!deletedSlots.contains(slotKey)) {
                    cylinderRepo.deleteByPartyDateCtypeAndFullType(partyName, date, ctype, status.toUpperCase());
                    deletedSlots.add(slotKey);
                }

                // Duplicate within this batch
                if (seenInBatch.contains(cylinderNo)) {
                    warnings.add("Duplicate in batch — cylinder " + cylinderNo + " skipped.");
                    continue;
                }
                seenInBatch.add(cylinderNo);

                // Warn if FULL and last status was also FULL
                if ("FULL".equalsIgnoreCase(status)) {
                    String lastStatus = cylinderRepo.getCylinderStatus(cylinderNo);
                    if ("FULL".equalsIgnoreCase(lastStatus)) {
                        List<Long> empties = entryService.getCylindersFullAtParty(partyName, ctype);
                        warnings.add("DUPLICATE_FULL:" + cylinderNo + ":" +
                                     String.join(",", empties.stream().map(String::valueOf).toList()));
                    }
                }

                // Warn if EMPTY and cylinder was last held by a different party
                if ("EMPTY".equalsIgnoreCase(status)) {
                    String lastCustomer = cylinderRepo.getCylinderHoldingStatus(cylinderNo);
                    if (lastCustomer != null && !lastCustomer.equalsIgnoreCase(partyName)) {
                        warnings.add("PARTY_MISMATCH:" + cylinderNo + ":" + lastCustomer + ":" + partyName);
                    }
                }

                MainCylinderEntry ce = new MainCylinderEntry();
                ce.setCylinderNo(cylinderNo);
                ce.setCustomerName(partyName);
                ce.setCtype(ctype);
                ce.setFullType(status);   // FULL or EMPTY — persisted separately from gas type
                ce.setDate(date);
                cylinderRepo.saveAndFlush(ce);
                saved++;
            } catch (Exception ex) {
                warnings.add("Error saving row: " + ex.getMessage());
                log.error("Cylinder save error", ex);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("saved",    saved);
        resp.put("warnings", warnings);
        return resp;
    }

    // ── View History — Holding View ───────────────────────────────────────────

    @GetMapping("/historyHolding")
    public String historyHolding(
            @RequestParam(required = false) String partyName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            Model model) {

        model.addAttribute("partyNames",    partyNamesRepo.getAllPartyNamesAll());
        model.addAttribute("selectedParty", partyName);
        model.addAttribute("fromDate", fromDate != null ? new SimpleDateFormat("yyyy-MM-dd").format(fromDate) : "");
        model.addAttribute("toDate",   toDate   != null ? new SimpleDateFormat("yyyy-MM-dd").format(toDate)   : "");

        boolean hasRange = fromDate != null && toDate != null;

        if (partyName != null && !partyName.isBlank()) {
            Map<String, List<MainEntry>> byType = new LinkedHashMap<>();
            for (CylinderTypeF t : CylinderTypeF.values()) {
                List<MainEntry> entries = hasRange
                    ? mainEntryRepo.findByPartyTypeAndDateRange(partyName, t.name(), fromDate, toDate)
                    : mainEntryRepo.findByPartyNameAndType(partyName, t.name());
                if (!entries.isEmpty()) byType.put(t.name(), entries);
            }
            model.addAttribute("byType", byType);
        } else {
            model.addAttribute("holdingSummary", hasRange
                ? mainEntryRepo.getCurrentHoldingSummaryForRange(fromDate, toDate)
                : mainEntryRepo.getCurrentHoldingSummary());
        }
        return "historyHolding";
    }

    // ── View History — Detail View ────────────────────────────────────────────

    @GetMapping("/historyDetail")
    public String historyDetail(
            @RequestParam(required = false) String partyName,
            @RequestParam(required = false) Long cylinderNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            Model model, HttpServletRequest request) {

        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        model.addAttribute("partyNames",       partyNamesRepo.getAllPartyNamesAll());
        model.addAttribute("selectedParty",    partyName);
        model.addAttribute("selectedCylinder", cylinderNo);
        model.addAttribute("fromDate", fromDate != null ? new SimpleDateFormat("yyyy-MM-dd").format(fromDate) : "");
        model.addAttribute("toDate",   toDate   != null ? new SimpleDateFormat("yyyy-MM-dd").format(toDate)   : "");

        boolean hasRange = fromDate != null && toDate != null;

        if (flash != null && flash.get("history") != null) {
            model.addAttribute("history",          flash.get("history"));
            model.addAttribute("viewMode",         flash.get("viewMode") != null ? flash.get("viewMode") : "party");
            model.addAttribute("issueReadingData", flash.get("issueReadingData"));
            model.addAttribute("inRotationResult", flash.get("inRotationResult"));
            model.addAttribute("offsetDate",       flash.get("offsetDate"));
        } else if (partyName != null && !partyName.isBlank()) {
            model.addAttribute("history", hasRange
                ? cylinderRepo.findAllByPartyAndDateRange(partyName, fromDate, toDate)
                : cylinderRepo.findAllByParty(partyName));
            model.addAttribute("viewMode", "party");
        } else if (cylinderNo != null) {
            model.addAttribute("history", hasRange
                ? cylinderRepo.findAllByCylinderNoAndDateRange(cylinderNo, fromDate, toDate)
                : cylinderRepo.findAllByCylinderNo(cylinderNo));
            model.addAttribute("viewMode", "cylinder");
        }
        return "historyDetail";
    }

    @GetMapping("/notInRotation")
    public RedirectView notInRotation(RedirectAttributes ra) {
        LocalDate offsetDate = LocalDate.now().minusDays(15);
        Date dateDate = Date.from(offsetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        try {
            ra.addFlashAttribute("offsetDate", dateDate);
            ra.addFlashAttribute("history",    cylinderRepo.getCylindersNotInRotation(dateDate));
            ra.addFlashAttribute("issueReadingData", false);
            ra.addFlashAttribute("inRotationResult", true);
        } catch (Exception e) {
            ra.addFlashAttribute("issueReadingData", true);
        }
        return new RedirectView("/historyDetail", true);
    }

    // ── Daily Report ──────────────────────────────────────────────────────────

    @GetMapping("/historyDaily")
    public String historyDaily(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            Model model) {
        Date reportDate = (date != null) ? date
            : Date.from(LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant());
        model.addAttribute("reportDate", reportDate);
        model.addAttribute("entries",    mainEntryRepo.findEntriesByCreatedDate(reportDate));
        return "historyDaily";
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    @GetMapping("/exportF")
    public void exportCSV(@RequestParam String partyName,
                          HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        String filename = partyName.replaceAll("[^a-zA-Z0-9]","_") + "_" +
                          new SimpleDateFormat("dd-MM-yyyy").format(new Date()) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        List<MainEntry> entries = mainEntryRepo.findByPartyName(
            partyName, Sort.by(Sort.Direction.ASC, "date"));

        ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
        csvWriter.writeHeader("Date","Party Name","Cylinder Type","Full","Empty","Holding","Remarks");
        String[] mapping    = {"date","partyName","ctype","cfull","cempty","cholding","remarks"};
        CellProcessor[] proc = {new FmtDate("dd-MMM-yy"), new NotNull(), new NotNull(),
                                new NotNull(), new NotNull(), new NotNull(),
                                new org.supercsv.cellprocessor.Optional()};

        Integer currentMonth = null;
        for (MainEntry e : entries) {
            int month = e.getMonthValue();
            if (currentMonth != null && !currentMonth.equals(month)) {
                csvWriter.write(new MainEntry(), mapping); // blank line between months
            }
            currentMonth = month;
            csvWriter.write(e, mapping, proc);
        }
        csvWriter.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int parseInt(String s, int def) {
        try { return s != null && !s.isBlank() ? Integer.parseInt(s.trim()) : def; }
        catch (NumberFormatException e) { return def; }
    }

    private Date parseDate(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(s);
    }
}
