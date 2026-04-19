package com.example.datn_sd_29.report.service;

import com.example.datn_sd_29.report.dto.ProductReportResponse;
import com.example.datn_sd_29.report.dto.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    public byte[] exportRevenueReport(RevenueReportResponse report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo Doanh thu");
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dataLabelStyle = createDataLabelStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            int rowNum = 0;
            
            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DOANH THU");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            
            rowNum++; // Empty row
            
            // Date range
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateLabel1 = dateRow.createCell(0);
            dateLabel1.setCellValue("Từ ngày:");
            dateLabel1.setCellStyle(dataLabelStyle);
            Cell dateValue1 = dateRow.createCell(1);
            dateValue1.setCellValue(report.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateValue1.setCellStyle(dateStyle);
            Cell dateLabel2 = dateRow.createCell(2);
            dateLabel2.setCellValue("Đến ngày:");
            dateLabel2.setCellStyle(dataLabelStyle);
            Cell dateValue2 = dateRow.createCell(3);
            dateValue2.setCellValue(report.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateValue2.setCellStyle(dateStyle);
            
            rowNum++; // Empty row
            
            // Summary section
            Row summaryHeaderRow = sheet.createRow(rowNum++);
            Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
            summaryHeaderCell.setCellValue("TỔNG QUAN");
            summaryHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
            
            createDataRow(sheet, rowNum++, "Tổng doanh thu:", report.getTotalRevenue().doubleValue(), dataLabelStyle, moneyStyle);
            createDataRow(sheet, rowNum++, "Tổng hóa đơn:", (double) report.getTotalInvoices(), dataLabelStyle, dataStyle);
            createDataRow(sheet, rowNum++, "Giá trị TB/đơn:", report.getAverageOrderValue().doubleValue(), dataLabelStyle, moneyStyle);
            createDataRow(sheet, rowNum++, "Doanh thu tiền mặt:", report.getCashRevenue().doubleValue(), dataLabelStyle, moneyStyle);
            createDataRow(sheet, rowNum++, "Doanh thu chuyển khoản:", report.getTransferRevenue().doubleValue(), dataLabelStyle, moneyStyle);
            
            rowNum++; // Empty row
            
            // Payment methods section
            Row pmHeaderRow = sheet.createRow(rowNum++);
            Cell pmHeaderCell = pmHeaderRow.createCell(0);
            pmHeaderCell.setCellValue("PHƯƠNG THỨC THANH TOÁN");
            pmHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
            
            Row pmColumnRow = sheet.createRow(rowNum++);
            pmColumnRow.createCell(0).setCellValue("Phương thức");
            pmColumnRow.createCell(1).setCellValue("Số đơn");
            pmColumnRow.createCell(2).setCellValue("Doanh thu");
            pmColumnRow.createCell(3).setCellValue("Tỷ lệ %");
            for (int i = 0; i < 4; i++) {
                pmColumnRow.getCell(i).setCellStyle(headerStyle);
            }
            
            for (RevenueReportResponse.PaymentMethodBreakdown pm : report.getPaymentMethods()) {
                Row pmRow = sheet.createRow(rowNum++);
                Cell methodCell = pmRow.createCell(0);
                methodCell.setCellValue(getPaymentMethodName(pm.getMethod()));
                methodCell.setCellStyle(dataStyle);
                Cell countCell = pmRow.createCell(1);
                countCell.setCellValue(pm.getCount());
                countCell.setCellStyle(dataStyle);
                Cell amountCell = pmRow.createCell(2);
                amountCell.setCellValue(pm.getAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);
                Cell percentCell = pmRow.createCell(3);
                percentCell.setCellValue(String.format("%.1f%%", pm.getPercentage()));
                percentCell.setCellStyle(dataStyle);
            }
            
            rowNum++; // Empty row
            
            // Invoice channels section
            Row icHeaderRow = sheet.createRow(rowNum++);
            Cell icHeaderCell = icHeaderRow.createCell(0);
            icHeaderCell.setCellValue("LOẠI HÓA ĐƠN");
            icHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
            
            Row icColumnRow = sheet.createRow(rowNum++);
            icColumnRow.createCell(0).setCellValue("Loại");
            icColumnRow.createCell(1).setCellValue("Số đơn");
            icColumnRow.createCell(2).setCellValue("Doanh thu");
            icColumnRow.createCell(3).setCellValue("Tỷ lệ %");
            for (int i = 0; i < 4; i++) {
                icColumnRow.getCell(i).setCellStyle(headerStyle);
            }
            
            if (report.getInvoiceChannels() != null) {
                for (RevenueReportResponse.InvoiceChannelBreakdown ic : report.getInvoiceChannels()) {
                    Row icRow = sheet.createRow(rowNum++);
                    Cell channelCell = icRow.createCell(0);
                    channelCell.setCellValue(getInvoiceChannelName(ic.getChannel()));
                    channelCell.setCellStyle(dataStyle);
                    Cell countCell = icRow.createCell(1);
                    countCell.setCellValue(ic.getCount());
                    countCell.setCellStyle(dataStyle);
                    Cell amountCell = icRow.createCell(2);
                    amountCell.setCellValue(ic.getAmount().doubleValue());
                    amountCell.setCellStyle(moneyStyle);
                    Cell percentCell = icRow.createCell(3);
                    percentCell.setCellValue(String.format("%.1f%%", ic.getPercentage()));
                    percentCell.setCellStyle(dataStyle);
                }
            }
            
            rowNum++; // Empty row
            
            // Daily revenue section
            Row dailyHeaderRow = sheet.createRow(rowNum++);
            Cell dailyHeaderCell = dailyHeaderRow.createCell(0);
            dailyHeaderCell.setCellValue("DOANH THU THEO NGÀY");
            dailyHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
            
            Row dailyColumnRow = sheet.createRow(rowNum++);
            dailyColumnRow.createCell(0).setCellValue("Ngày");
            dailyColumnRow.createCell(1).setCellValue("Doanh thu");
            dailyColumnRow.createCell(2).setCellValue("Số hóa đơn");
            for (int i = 0; i < 3; i++) {
                dailyColumnRow.getCell(i).setCellStyle(headerStyle);
            }
            
            for (RevenueReportResponse.DailyRevenue daily : report.getDailyRevenues()) {
                Row dailyRow = sheet.createRow(rowNum++);
                Cell dateCell = dailyRow.createCell(0);
                dateCell.setCellValue(daily.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                dateCell.setCellStyle(dateStyle);
                Cell revenueCell = dailyRow.createCell(1);
                revenueCell.setCellValue(daily.getRevenue().doubleValue());
                revenueCell.setCellStyle(moneyStyle);
                Cell invoiceCell = dailyRow.createCell(2);
                invoiceCell.setCellValue(daily.getInvoiceCount());
                invoiceCell.setCellStyle(dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportProductReport(ProductReportResponse report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo Món ăn");
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dataLabelStyle = createDataLabelStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            int rowNum = 0;
            
            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO MÓN ĂN BÁN CHẠY");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            
            rowNum++; // Empty row
            
            // Date range
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateLabel1 = dateRow.createCell(0);
            dateLabel1.setCellValue("Từ ngày:");
            dateLabel1.setCellStyle(dataLabelStyle);
            Cell dateValue1 = dateRow.createCell(1);
            dateValue1.setCellValue(report.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateValue1.setCellStyle(dateStyle);
            Cell dateLabel2 = dateRow.createCell(2);
            dateLabel2.setCellValue("Đến ngày:");
            dateLabel2.setCellStyle(dataLabelStyle);
            Cell dateValue2 = dateRow.createCell(3);
            dateValue2.setCellValue(report.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateValue2.setCellStyle(dateStyle);
            
            rowNum++; // Empty row
            
            // Top by revenue section
            Row revHeaderRow = sheet.createRow(rowNum++);
            Cell revHeaderCell = revHeaderRow.createCell(0);
            revHeaderCell.setCellValue("TOP MÓN ĂN THEO DOANH THU");
            revHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row revColumnRow = sheet.createRow(rowNum++);
            revColumnRow.createCell(0).setCellValue("Hạng");
            revColumnRow.createCell(1).setCellValue("Tên món");
            revColumnRow.createCell(2).setCellValue("Danh mục");
            revColumnRow.createCell(3).setCellValue("Số lượng");
            revColumnRow.createCell(4).setCellValue("Doanh thu");
            for (int i = 0; i < 5; i++) {
                revColumnRow.getCell(i).setCellStyle(headerStyle);
            }
            
            int rank = 1;
            for (ProductReportResponse.ProductSales product : report.getTopByRevenue()) {
                Row productRow = sheet.createRow(rowNum++);
                Cell rankCell = productRow.createCell(0);
                rankCell.setCellValue(rank++);
                rankCell.setCellStyle(dataStyle);
                Cell nameCell = productRow.createCell(1);
                nameCell.setCellValue(product.getProductName());
                nameCell.setCellStyle(dataStyle);
                Cell categoryCell = productRow.createCell(2);
                categoryCell.setCellValue(getCategoryName(product.getCategory()));
                categoryCell.setCellStyle(dataStyle);
                Cell qtyCell = productRow.createCell(3);
                qtyCell.setCellValue(product.getQuantitySold());
                qtyCell.setCellStyle(dataStyle);
                Cell revenueCell = productRow.createCell(4);
                revenueCell.setCellValue(product.getRevenue().doubleValue());
                revenueCell.setCellStyle(moneyStyle);
            }
            
            rowNum++; // Empty row
            
            // Top by quantity section
            Row qtyHeaderRow = sheet.createRow(rowNum++);
            Cell qtyHeaderCell = qtyHeaderRow.createCell(0);
            qtyHeaderCell.setCellValue("TOP MÓN ĂN THEO SỐ LƯỢNG");
            qtyHeaderCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            
            Row qtyColumnRow = sheet.createRow(rowNum++);
            qtyColumnRow.createCell(0).setCellValue("Hạng");
            qtyColumnRow.createCell(1).setCellValue("Tên món");
            qtyColumnRow.createCell(2).setCellValue("Danh mục");
            qtyColumnRow.createCell(3).setCellValue("Số lượng");
            qtyColumnRow.createCell(4).setCellValue("Doanh thu");
            for (int i = 0; i < 5; i++) {
                qtyColumnRow.getCell(i).setCellStyle(headerStyle);
            }
            
            rank = 1;
            for (ProductReportResponse.ProductSales product : report.getTopByQuantity()) {
                Row productRow = sheet.createRow(rowNum++);
                Cell rankCell = productRow.createCell(0);
                rankCell.setCellValue(rank++);
                rankCell.setCellStyle(dataStyle);
                Cell nameCell = productRow.createCell(1);
                nameCell.setCellValue(product.getProductName());
                nameCell.setCellStyle(dataStyle);
                Cell categoryCell = productRow.createCell(2);
                categoryCell.setCellValue(getCategoryName(product.getCategory()));
                categoryCell.setCellStyle(dataStyle);
                Cell qtyCell = productRow.createCell(3);
                qtyCell.setCellValue(product.getQuantitySold());
                qtyCell.setCellStyle(dataStyle);
                Cell revenueCell = productRow.createCell(4);
                revenueCell.setCellValue(product.getRevenue().doubleValue());
                revenueCell.setCellStyle(moneyStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataLabelStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createDataRow(Sheet sheet, int rowNum, String label, double value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f", amount.doubleValue());
    }

    private String getPaymentMethodName(String method) {
        switch (method) {
            case "CASH": return "Tiền mặt";
            case "TRANSFER": return "Chuyển khoản";
            case "CARD": return "Thẻ";
            case "POINTS": return "Điểm tích lũy";
            default: return method;
        }
    }

    private String getInvoiceChannelName(String channel) {
        switch (channel) {
            case "ONLINE": return "Online";
            case "OFFLINE": return "Tại quầy";
            default: return channel;
        }
    }

    private String getCategoryName(String category) {
        switch (category) {
            case "RAW_FOOD": return "Thực phẩm sống";
            case "COOKED_FOOD": return "Món ăn chín";
            case "HOT_POT_BROTH": return "Nước lẩu";
            case "DRINK": return "Đồ uống";
            case "DESSERT": return "Tráng miệng";
            default: return category;
        }
    }
}
