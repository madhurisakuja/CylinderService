package com.cylindertrack.app.model;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class ExcelBillGenerator {

    private static final String BIZ_NAME    = "B.K ENTERPRISES";
    private static final String BIZ_GSTIN   = "GSTIN: 20ALFPS1571A1ZP; STATE CODE: 20; STATE: JHARKHAND.";
    private static final String BIZ_ADDR    = "LALBABA FOUNDRY, BURMAMINES, JAMSHEDPUR. 831007";
    private static final String BIZ_CONTACT = "PH NO:7209583688; EMAIL-BKGASCRANE@GMAIL.COM.";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM.yyyy");

    public byte[] generate(List<BillSummary> bills) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFFont boldFont = wb.createFont();
        boldFont.setBold(true); boldFont.setFontHeightInPoints((short)11); boldFont.setFontName("Arial");
        XSSFFont normalFont = wb.createFont();
        normalFont.setFontHeightInPoints((short)10); normalFont.setFontName("Arial");
        XSSFFont bigBoldFont = wb.createFont();
        bigBoldFont.setBold(true); bigBoldFont.setFontHeightInPoints((short)14); bigBoldFont.setFontName("Arial");

        // Pre-build shared styles once for the workbook
        XSSFCellStyle numStyle  = numericStyle(wb, normalFont, "#,##0.00");
        XSSFCellStyle pctStyle  = wb.createCellStyle();
        pctStyle.setFont(normalFont);
        pctStyle.setAlignment(HorizontalAlignment.CENTER);
        // Store "18%" as a plain string — avoids the 0.18 rendering issue
        XSSFCellStyle borderStyle = bordered(wb, normalFont);
        XSSFCellStyle hdrStyle    = headerStyle(wb, boldFont);

        for (BillSummary bill : bills) {
            String raw = sanitize(bill.getPartyName()) + " " +
                         bill.getInvoiceNumber().replace("/", "-");
            String sheetName = raw.length() > 31 ? raw.substring(0, 31) : raw;
            XSSFSheet ws = wb.createSheet(sheetName);

            ws.setColumnWidth(0, 6  * 256);
            ws.setColumnWidth(1, 30 * 256);  // wider for cylinder numbers
            ws.setColumnWidth(2, 14 * 256);
            ws.setColumnWidth(3, 8  * 256);
            ws.setColumnWidth(4, 10 * 256);
            ws.setColumnWidth(5, 12 * 256);
            ws.setColumnWidth(6, 14 * 256);
            ws.setColumnWidth(7, 14 * 256);
            ws.setColumnWidth(8, 14 * 256);
            ws.setColumnWidth(9, 14 * 256);

            int r = 0;

            // Row 1 — ORIGINAL COPY
            cell(ws.createRow(r++), 8, "ORIGINAL COPY", normalFont, HorizontalAlignment.RIGHT);

            // Row 2 — Business name
            cell(ws.createRow(r++), 0, BIZ_NAME, bigBoldFont, HorizontalAlignment.LEFT);
            r++; r++; // blank 3,4

            // Row 5 — GSTIN
            cell(ws.createRow(r++), 0, BIZ_GSTIN, normalFont, HorizontalAlignment.LEFT);
            // Row 6 — Address
            cell(ws.createRow(r++), 1, BIZ_ADDR, normalFont, HorizontalAlignment.LEFT);
            // Row 7 — Contact
            cell(ws.createRow(r++), 0, BIZ_CONTACT, normalFont, HorizontalAlignment.LEFT);
            // Row 8 — TAX INVOICE
            cell(ws.createRow(r++), 3, "TAX INVOICE", bigBoldFont, HorizontalAlignment.CENTER);
            r++; // blank

            // Row 10 — Buyer + Invoice No
            Row row10 = ws.createRow(r++);
            cell(row10, 0, "Buyer:", boldFont, HorizontalAlignment.LEFT);
            cell(row10, 5, "INVOICE NO: " + bill.getInvoiceNumber(), boldFont, HorizontalAlignment.LEFT);

            // Row 11 — Party name + Invoice Date (today)
            Row row11 = ws.createRow(r++);
            cell(row11, 0, bill.getPartyName(), boldFont, HorizontalAlignment.LEFT);
            cell(row11, 5, "INVOICE DATE: " + DATE_FMT.format(bill.getInvoiceDate()),
                 normalFont, HorizontalAlignment.LEFT);

            // Party address block
            PartyAccount pa = bill.getPartyAccount();
            if (pa != null) {
                if (notBlank(pa.getAddress())) {
                    for (String line : splitAddress(pa.getAddress()))
                        cell(ws.createRow(r++), 0, line, normalFont, HorizontalAlignment.LEFT);
                }
                if (notBlank(pa.getGstin()))
                    cell(ws.createRow(r++), 0, "GSTIN: " + pa.getGstin(), normalFont, HorizontalAlignment.LEFT);
                if (notBlank(pa.getStateCode())) {
                    String sc = "STATE CODE: " + pa.getStateCode();
                    if (notBlank(pa.getStateName())) sc += "   STATE: " + pa.getStateName();
                    cell(ws.createRow(r++), 0, sc, normalFont, HorizontalAlignment.LEFT);
                }
            }
            r++; // blank before header

            // Table header
            Row hdr = ws.createRow(r++);
            String[] headers = {"SL NO","Particulars","HSN code","GST %","Quantity","Rate",
                                 "Taxable","CGST Amt","SGST Amt","AMOUNT"};
            for (int i = 0; i < headers.length; i++) {
                XSSFCell c = (XSSFCell) hdr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hdrStyle);
            }

            // Sub-header: Amount / 9% / 9%  (string, not numeric)
            Row subHdr = ws.createRow(r++);
            cell(subHdr, 6, "Amount", normalFont, HorizontalAlignment.CENTER);
            cell(subHdr, 7, "9%",     normalFont, HorizontalAlignment.CENTER);
            cell(subHdr, 8, "9%",     normalFont, HorizontalAlignment.CENTER);

            // Data rows
            List<BillLineItem> items = bill.getLineItems();
            int[] dataRows = new int[items.size()];

            for (int i = 0; i < items.size(); i++) {
                BillLineItem item = items.get(i);
                Row dr = ws.createRow(r);
                dataRows[i] = r + 1;
                r++;

                setCellInt(dr, 0, item.getSlNo(), borderStyle);

                // Particulars + cylinder numbers on next line if available
                String particulars = item.getParticulars();
                if (!item.getCylinderNumbers().isBlank())
                    particulars += "\n" + item.getCylinderNumbers();
                XSSFCell partCell = (XSSFCell) dr.createCell(1);
                partCell.setCellValue(particulars);
                partCell.setCellStyle(borderStyle);
                if (!item.getCylinderNumbers().isBlank()) {
                    dr.setHeight((short)(dr.getHeight() * 2));
                }

                setCell(dr, 2, notBlankStr(item.getHsnCode()), borderStyle);

                // GST % as string "18%"
                XSSFCell gstCell = (XSSFCell) dr.createCell(3);
                gstCell.setCellValue("18%");
                gstCell.setCellStyle(borderStyle);

                setCellInt(dr, 4, item.getQuantity(), borderStyle);
                setCellNum(dr, 5, item.getRate(), numStyle);

                int eRow = r;
                dr.createCell(6).setCellFormula("F" + eRow + "*E" + eRow);
                dr.getCell(6).setCellStyle(numStyle);
                dr.createCell(7).setCellFormula("G" + eRow + "*0.09");
                dr.getCell(7).setCellStyle(numStyle);
                dr.createCell(8).setCellFormula("G" + eRow + "*0.09");
                dr.getCell(8).setCellStyle(numStyle);
                dr.createCell(9).setCellFormula("I" + eRow + "+H" + eRow + "+G" + eRow);
                dr.getCell(9).setCellStyle(numStyle);
            }

            r += 3; // spacing before totals

            // Total
            Row totRow = ws.createRow(r++);
            cell(totRow, 8, "Total", boldFont, HorizontalAlignment.RIGHT);
            StringBuilder jSum = new StringBuilder();
            for (int i = 0; i < dataRows.length; i++) {
                if (i > 0) jSum.append("+");
                jSum.append("J").append(dataRows[i]);
            }
            totRow.createCell(9).setCellFormula(jSum.toString());
            totRow.getCell(9).setCellStyle(numStyle);

            // T/C (optional)
            if (bill.hasTc()) {
                Row tcRow = ws.createRow(r++);
                cell(tcRow, 8, "T/C", normalFont, HorizontalAlignment.RIGHT);
                setCellNum(tcRow, 9, bill.getTcCharge(), numStyle);
            }

            // Security (optional)
            if (bill.hasSecurity()) {
                Row secRow = ws.createRow(r++);
                cell(secRow, 8, "Security Adj.", normalFont, HorizontalAlignment.RIGHT);
                setCellNum(secRow, 9, bill.getSecurityDeposit().negate(), numStyle);
            }

            // Discount (optional, private)
            if (bill.hasDiscount()) {
                Row discRow = ws.createRow(r++);
                cell(discRow, 8, "Discount", normalFont, HorizontalAlignment.RIGHT);
                setCellNum(discRow, 9, bill.getDiscountAmount().negate(), numStyle);
            }

            // Net Total
            Row netRow = ws.createRow(r++);
            cell(netRow, 8, "Net Total", boldFont, HorizontalAlignment.RIGHT);
            setCellNum(netRow, 9, bill.getGrandTotal(), numStyle);

            r++; // blank

            // Amount in words
            cell(ws.createRow(r++), 0,
                 "AMOUNT IN WORDS: " + bill.getAmountInWords() + ".",
                 boldFont, HorizontalAlignment.LEFT);

            r++;
            cell(ws.createRow(r++), 0, BIZ_NAME, boldFont, HorizontalAlignment.LEFT);
            r += 2;
            cell(ws.createRow(r), 0, "AUTHORISED SIGNATORY", normalFont, HorizontalAlignment.LEFT);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void cell(Row row, int col, String val, XSSFFont font, HorizontalAlignment align) {
        XSSFWorkbook wb = (XSSFWorkbook) row.getSheet().getWorkbook();
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setAlignment(align);
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(s);
    }

    private void setCell(Row row, int col, String val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private void setCellInt(Row row, int col, int val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private void setCellNum(Row row, int col, BigDecimal val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val.doubleValue()); c.setCellStyle(style);
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private XSSFCellStyle numericStyle(XSSFWorkbook wb, XSSFFont font, String fmt) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font); s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat(fmt));
        return s;
    }

    private XSSFCellStyle bordered(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String notBlankStr(String s) { return s != null ? s : ""; }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\[\\]]", "").trim();
    }

    private String[] splitAddress(String addr) {
        if (addr.length() <= 40) return new String[]{addr};
        int mid = addr.lastIndexOf(',', 40);
        if (mid < 0) mid = 40;
        String first = addr.substring(0, mid + 1).trim();
        String rest  = addr.substring(mid + 1).trim();
        if (rest.length() <= 40) return new String[]{first, rest};
        int mid2 = rest.lastIndexOf(',', 40);
        if (mid2 < 0) mid2 = 40;
        return new String[]{first, rest.substring(0, mid2+1).trim(), rest.substring(mid2+1).trim()};
    }
}
