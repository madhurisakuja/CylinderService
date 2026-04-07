package com.cylindertrack.app.model;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Generates an Excel workbook matching the B.K ENTERPRISES bill format.
 * One sheet per party. Uses Apache POI (bundled with Spring Boot).
 */
@Component
public class ExcelBillGenerator {

    private static final String BIZ_NAME    = "B.K ENTERPRISES";
    private static final String BIZ_GSTIN   = "GSTIN: 20ALFPS1571A1ZP; STATE CODE: 20; STATE: JHARKHAND.";
    private static final String BIZ_ADDR    = "LALBABA FOUNDRY, BURMAMINES, JAMSHEDPUR. 831007";
    private static final String BIZ_CONTACT = "PH NO:7209583688; EMAIL-BKGASCRANE@GMAIL.COM.";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM.yyyy");

    public byte[] generate(List<BillSummary> bills) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();

        // Shared styles
        XSSFFont boldFont = wb.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 11);
        boldFont.setFontName("Arial");

        XSSFFont normalFont = wb.createFont();
        normalFont.setFontHeightInPoints((short) 10);
        normalFont.setFontName("Arial");

        XSSFFont bigBoldFont = wb.createFont();
        bigBoldFont.setBold(true);
        bigBoldFont.setFontHeightInPoints((short) 14);
        bigBoldFont.setFontName("Arial");

        for (BillSummary bill : bills) {
            String sheetName = sanitize(bill.getPartyName()) + " " +
                               bill.getInvoiceNumber().replace("/", "-");
            XSSFSheet ws = wb.createSheet(sheetName.length() > 31
                ? sheetName.substring(0, 31) : sheetName);

            // Column widths (in units of 1/256 of a character width)
            ws.setColumnWidth(0, 6 * 256);   // A - SL NO
            ws.setColumnWidth(1, 28 * 256);  // B - Particulars
            ws.setColumnWidth(2, 14 * 256);  // C - HSN
            ws.setColumnWidth(3, 8 * 256);   // D - GST%
            ws.setColumnWidth(4, 10 * 256);  // E - Qty
            ws.setColumnWidth(5, 12 * 256);  // F - Rate
            ws.setColumnWidth(6, 14 * 256);  // G - Taxable
            ws.setColumnWidth(7, 14 * 256);  // H - CGST
            ws.setColumnWidth(8, 14 * 256);  // I - SGST
            ws.setColumnWidth(9, 14 * 256);  // J - Amount

            int r = 0; // current row (0-indexed)

            // Row 1 - ORIGINAL COPY in col I
            Row row1 = ws.createRow(r++);
            cell(row1, 8, "ORIGINAL COPY", normalFont, null, HorizontalAlignment.RIGHT);

            // Row 2 - Business name
            Row row2 = ws.createRow(r++);
            XSSFCellStyle bizStyle = wb.createCellStyle();
            bizStyle.setFont(bigBoldFont);
            cell(row2, 0, BIZ_NAME, bigBoldFont, null, HorizontalAlignment.LEFT);

            r++; // blank row 3
            r++; // blank row 4

            // Row 5 - GSTIN
            Row row5 = ws.createRow(r++);
            cell(row5, 0, BIZ_GSTIN, normalFont, null, HorizontalAlignment.LEFT);

            // Row 6 - Address
            Row row6 = ws.createRow(r++);
            cell(row6, 1, BIZ_ADDR, normalFont, null, HorizontalAlignment.LEFT);

            // Row 7 - Phone/email
            Row row7 = ws.createRow(r++);
            cell(row7, 0, BIZ_CONTACT, normalFont, null, HorizontalAlignment.LEFT);

            // Row 8 - TAX INVOICE centred
            Row row8 = ws.createRow(r++);
            XSSFCellStyle invTitleStyle = wb.createCellStyle();
            invTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            invTitleStyle.setFont(bigBoldFont);
            cell(row8, 3, "TAX INVOICE", bigBoldFont, null, HorizontalAlignment.CENTER);

            // Row 9 - blank
            r++;

            // Row 10 - Buyer label + Invoice No
            Row row10 = ws.createRow(r++);
            cell(row10, 0, "Buyer:", boldFont, null, HorizontalAlignment.LEFT);
            cell(row10, 5, "INVOICE NO: " + bill.getInvoiceNumber(), boldFont, null, HorizontalAlignment.LEFT);

            // Row 11 - Party name + Invoice Date
            Row row11 = ws.createRow(r++);
            cell(row11, 0, bill.getPartyName(), boldFont, null, HorizontalAlignment.LEFT);
            cell(row11, 5, "INVOICE DATE: " + DATE_FMT.format(bill.getInvoiceDate()),
                 normalFont, null, HorizontalAlignment.LEFT);

            // Rows 12-17 - Party address details
            PartyAccount pa = bill.getPartyAccount();
            if (pa != null) {
                if (pa.getAddress() != null && !pa.getAddress().isBlank()) {
                    // Split address into max 3 lines of ~40 chars
                    String[] addrLines = splitAddress(pa.getAddress());
                    for (String line : addrLines) {
                        Row ar = ws.createRow(r++);
                        cell(ar, 0, line, normalFont, null, HorizontalAlignment.LEFT);
                    }
                }
                if (pa.getGstin() != null && !pa.getGstin().isBlank()) {
                    Row gr = ws.createRow(r++);
                    cell(gr, 0, "GSTIN: " + pa.getGstin(), normalFont, null, HorizontalAlignment.LEFT);
                }
                if (pa.getStateCode() != null && !pa.getStateCode().isBlank()) {
                    Row sr = ws.createRow(r++);
                    String sc = "STATE CODE: " + pa.getStateCode();
                    if (pa.getStateName() != null) sc += "   STATE: " + pa.getStateName();
                    cell(sr, 0, sc, normalFont, null, HorizontalAlignment.LEFT);
                }
            }

            r++; // blank before table header

            // Table header row
            Row hdr = ws.createRow(r++);
            XSSFCellStyle hdrStyle = headerStyle(wb, boldFont);
            String[] headers = {"SL NO","Particulars","HSN code","GST %","Quantity","Rate",
                                "Taxable","CGST Amt","SGST Amt","AMOUNT"};
            for (int i = 0; i < headers.length; i++) {
                XSSFCell c = (XSSFCell) hdr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hdrStyle);
            }

            // Sub-header (Amount / 0.09 / 0.09)
            Row subHdr = ws.createRow(r++);
            cell(subHdr, 6, "Amount", normalFont, null, HorizontalAlignment.CENTER);
            subHdr.createCell(7).setCellValue(0.09);
            subHdr.createCell(8).setCellValue(0.09);

            // Data rows
            int firstDataRow = r + 1; // 1-indexed for formulas
            XSSFCellStyle numStyle   = numericStyle(wb, normalFont, "#,##0.00");
            XSSFCellStyle intStyle   = numericStyle(wb, normalFont, "#,##0");
            XSSFCellStyle pctStyle   = numericStyle(wb, normalFont, "0%");
            XSSFCellStyle borderStyle = bordered(wb, normalFont);

            List<BillLineItem> items = bill.getLineItems();
            int[] dataExcelRows = new int[items.size()]; // track for SUM formula

            for (int i = 0; i < items.size(); i++) {
                BillLineItem item = items.get(i);
                Row dr = ws.createRow(r);
                dataExcelRows[i] = r + 1; // 1-indexed
                r++;

                setCellInt(dr, 0, item.getSlNo(), borderStyle);
                setCell(dr, 1, item.getParticulars(), borderStyle);
                setCell(dr, 2, item.getHsnCode() != null ? item.getHsnCode() : "", borderStyle);
                dr.createCell(3).setCellValue(0.18);
                dr.getCell(3).setCellStyle(pctStyle);
                setCellInt(dr, 4, item.getQuantity(), borderStyle);
                setCellNum(dr, 5, item.getRate(), numStyle);

                int eRow = r; // excel row (1-indexed already r was pre-incremented)
                dr.createCell(6).setCellFormula("F" + eRow + "*E" + eRow);
                dr.getCell(6).setCellStyle(numStyle);
                dr.createCell(7).setCellFormula("G" + eRow + "*0.09");
                dr.getCell(7).setCellStyle(numStyle);
                dr.createCell(8).setCellFormula("G" + eRow + "*0.09");
                dr.getCell(8).setCellStyle(numStyle);
                dr.createCell(9).setCellFormula("I" + eRow + "+H" + eRow + "+G" + eRow);
                dr.getCell(9).setCellStyle(numStyle);
            }

            // Blank rows before totals (match sample spacing)
            r += 3;

            // Total row
            Row totRow = ws.createRow(r++);
            cell(totRow, 8, "Total", boldFont, null, HorizontalAlignment.RIGHT);
            StringBuilder jSum = new StringBuilder("=");
            for (int i = 0; i < dataExcelRows.length; i++) {
                if (i > 0) jSum.append("+");
                jSum.append("J").append(dataExcelRows[i]);
            }
            totRow.createCell(9).setCellFormula(jSum.toString().replace("=", ""));
            totRow.getCell(9).setCellStyle(numStyle);
            int totalExcelRow = r; // for reference below

            // T/C row (transport/labour — added)
            if (bill.hasTc()) {
                Row tcRow = ws.createRow(r++);
                cell(tcRow, 8, "T/C", normalFont, null, HorizontalAlignment.RIGHT);
                setCellNum(tcRow, 9, bill.getTcCharge(), numStyle);
            }

            // Security deposit row (deducted) — only shown when present
            if (bill.hasSecurity()) {
                Row secRow = ws.createRow(r++);
                cell(secRow, 8, "Security Adj.", normalFont, null, HorizontalAlignment.RIGHT);
                setCellNum(secRow, 9, bill.getSecurityDeposit().negate(), numStyle);
            }

            // Discount row — only shown when present (good-will, private)
            if (bill.hasDiscount()) {
                Row discRow = ws.createRow(r++);
                cell(discRow, 8, "Discount", normalFont, null, HorizontalAlignment.RIGHT);
                setCellNum(discRow, 9, bill.getDiscountAmount().negate(), numStyle);
            }

            // R/F (rounding figure)
            Row rfRow = ws.createRow(r++);
            cell(rfRow, 8, "R/F", normalFont, null, HorizontalAlignment.RIGHT);
            rfRow.createCell(9).setCellValue(0.00);

            // Net Total
            Row netRow = ws.createRow(r++);
            cell(netRow, 8, "Net Total", boldFont, null, HorizontalAlignment.RIGHT);
            setCellNum(netRow, 9, bill.getGrandTotal(), numStyle);
            int netExcelRow = r;

            r++; // blank

            // Amount in words
            Row wordsRow = ws.createRow(r++);
            cell(wordsRow, 0, "AMOUNT IN WORDS: " + bill.getAmountInWords() + ".",
                 boldFont, null, HorizontalAlignment.LEFT);

            r += 1; // blank

            // Business sign-off
            Row bizRow = ws.createRow(r++);
            cell(bizRow, 0, BIZ_NAME, boldFont, null, HorizontalAlignment.LEFT);

            r += 2;
            Row authRow = ws.createRow(r);
            cell(authRow, 0, "AUTHORISED SIGNATORY", normalFont, null, HorizontalAlignment.LEFT);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private void cell(Row row, int col, String val, XSSFFont font,
                      XSSFCellStyle style, HorizontalAlignment align) {
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val);
        if (style == null) {
            XSSFWorkbook wb = (XSSFWorkbook) row.getSheet().getWorkbook();
            style = wb.createCellStyle();
            style.setFont(font);
            style.setAlignment(align);
        }
        c.setCellStyle(style);
    }

    private void setCell(Row row, int col, String val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private void setCellInt(Row row, int col, int val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private void setCellNum(Row row, int col, BigDecimal val, XSSFCellStyle style) {
        XSSFCell c = (XSSFCell) row.createCell(col);
        c.setCellValue(val.doubleValue());
        c.setCellStyle(style);
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private XSSFCellStyle numericStyle(XSSFWorkbook wb, XSSFFont font, String fmt) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat(fmt));
        return s;
    }

    private XSSFCellStyle bordered(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

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
        return new String[]{first, rest.substring(0, mid2 + 1).trim(), rest.substring(mid2 + 1).trim()};
    }
}
