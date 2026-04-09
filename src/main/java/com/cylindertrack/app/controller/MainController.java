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
        model.addAttribute("allParties", partyNamesRepo.getAllPartyNamesAll());
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
        if (flash != null) model.addAttribute("deleteSuccess", flash.get("deleteSuccess"));
        Date defDate = mainEntryRepo.getLatestEntryDate();
        MainEntry entry = new MainEntry();
        if (defDate != null) entry.setDate(defDate);
        List<String> all = new ArrayList<>(partyNamesRepo.getAllPartyNamesPurchaser());
        all.addAll(partyNamesRepo.getAllPartyNames());
        model.addAttribute("partyNames", all);
        model.addAttribute("entry", entry);
        return "deleteEntryF";
    }

    @PostMapping("/deleteEntryF")
    public RedirectView deleteEntryPost(@ModelAttribute MainEntry entry, RedirectAttributes ra) {
        mainEntryRepo.deleteByPartyAndDate(entry.getPartyName(), entry.getDate());
        log.info("Deleted entries for party={} date={}", entry.getPartyName(), entry.getDate());
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
        return "cylinderNumbers";
    }

    /**
     * Loads bulk entries for party+date so the JS can render cylinder number input forms.
     * Also returns cylinders currently FULL at this party per type for EMPTY dropdown suggestions.
     */
    @GetMapping("/cylinderNumbers/load")
    @ResponseBody
    public Map<String, Object> loadCylinderNumbersForm(
            @RequestParam String partyName,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {

        List<MainEntry> bulkEntries = mainEntryRepo.findByPartyAndDate(partyName, date);

        // Build map: gasType → list of cylinder numbers currently FULL at this party
        Map<String, List<Long>> fullAtParty = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values()) {
            List<Long> cyls = entryService.getCylindersFullAtParty(partyName, t.name());
            if (!cyls.isEmpty()) fullAtParty.put(t.name(), cyls);
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
        return resp;
    }

    /** Save individual cylinder number entries */
    @PostMapping("/cylinderNumbers/save")
    @ResponseBody
    public Map<String, Object> saveCylinderNumbers(@RequestBody List<Map<String, String>> rows) {
        int saved = 0;
        List<String> warnings = new ArrayList<>();

        for (Map<String, String> row : rows) {
            try {
                Long cylinderNo = Long.parseLong(row.get("cylinderNo").trim());
                String partyName = row.get("partyName");
                String ctype     = row.get("ctype");
                Date   date      = parseDate(row.get("date"));
                String status    = row.get("status"); // FULL or EMPTY

                // Check if this is a new FULL for a cylinder whose last status is also FULL
                if ("FULL".equalsIgnoreCase(status)) {
                    String lastStatus = cylinderRepo.getCylinderStatus(cylinderNo);
                    if ("FULL".equalsIgnoreCase(lastStatus)) {
                        List<Long> empties = entryService.getCylindersFullAtParty(partyName, ctype);
                        warnings.add("DUPLICATE_FULL:" + cylinderNo + ":" +
                                     String.join(",", empties.stream().map(String::valueOf).toList()));
                    }
                }

                // Check EMPTY mismatch — cylinder sent to different party
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
                warnings.add("ERROR:" + ex.getMessage());
                log.error("Cylinder number save error", ex);
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
            Model model) {

        model.addAttribute("partyNames", partyNamesRepo.getAllPartyNamesAll());
        model.addAttribute("selectedParty", partyName);

        if (partyName != null && !partyName.isBlank()) {
            // All entries grouped by type for the selected party
            Map<String, List<MainEntry>> byType = new LinkedHashMap<>();
            for (CylinderTypeF t : CylinderTypeF.values()) {
                List<MainEntry> entries = mainEntryRepo.findByPartyNameAndType(partyName, t.name());
                if (!entries.isEmpty()) byType.put(t.name(), entries);
            }
            model.addAttribute("byType", byType);
        } else {
            // All-parties holding summary
            model.addAttribute("holdingSummary", mainEntryRepo.getCurrentHoldingSummary());
        }
        return "historyHolding";
    }

    // ── View History — Detail View ────────────────────────────────────────────

    @GetMapping("/historyDetail")
    public String historyDetail(
            @RequestParam(required = false) String partyName,
            @RequestParam(required = false) Long cylinderNo,
            Model model, HttpServletRequest request) {

        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        model.addAttribute("partyNames", partyNamesRepo.getAllPartyNamesAll());
        model.addAttribute("selectedParty",   partyName);
        model.addAttribute("selectedCylinder", cylinderNo);

        if (partyName != null && !partyName.isBlank()) {
            model.addAttribute("history", cylinderRepo.findAllByParty(partyName));
            model.addAttribute("viewMode", "party");
        } else if (cylinderNo != null) {
            model.addAttribute("history", cylinderRepo.findAllByCylinderNo(cylinderNo));
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
