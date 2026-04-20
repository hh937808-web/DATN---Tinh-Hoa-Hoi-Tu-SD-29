package com.example.datn_sd_29.report.service;

import com.example.datn_sd_29.dashboard.dto.DashboardStatsResponse;
import com.example.datn_sd_29.dashboard.dto.RecentInvoiceResponse;
import com.example.datn_sd_29.dashboard.dto.RevenueChartResponse;
import com.example.datn_sd_29.dashboard.dto.TopProductResponse;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    private String getStatusName(String status) {
        switch (status) {
            case "PAID": return "Đã thanh toán";
            case "IN_PROGRESS": return "Đang phục vụ";
            case "RESERVED": return "Đã đặt bàn";
            case "CANCELLED": return "Đã hủy";
            case "NO_SHOW": return "Không đến";
            default: return status;
        }
    }

    public byte[] exportDashboard(
            DashboardStatsResponse stats,
            RevenueChartResponse chartData,
            List<TopProductResponse> topProducts,
            List<RecentInvoiceResponse> recentInvoices,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dataLabelStyle = createDataLabelStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // ========== Sheet 1: Tổng quan ==========
            Sheet overviewSheet = workbook.createSheet("Tổng quan");
            int rowNum = 0;

            Row titleRow = overviewSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DASHBOARD");
            titleCell.setCellStyle(titleStyle);
            overviewSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            rowNum++;

            Row dateRow = overviewSheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Từ ngày:");
            dateRow.getCell(0).setCellStyle(dataLabelStyle);
            dateRow.createCell(1).setCellValue(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateRow.getCell(1).setCellStyle(dateStyle);
            dateRow.createCell(2).setCellValue("Đến ngày:");
            dateRow.getCell(2).setCellStyle(dataLabelStyle);
            dateRow.createCell(3).setCellValue(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dateRow.getCell(3).setCellStyle(dateStyle);

            rowNum++;

            Row statsHeader = overviewSheet.createRow(rowNum++);
            statsHeader.createCell(0).setCellValue("THỐNG KÊ TỔNG QUAN");
            statsHeader.getCell(0).setCellStyle(headerStyle);
            overviewSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            if (stats.getTodayRevenue() != null) {
                createDataRow(overviewSheet, rowNum++, "Doanh thu:", stats.getTodayRevenue().getAmount().doubleValue(), dataLabelStyle, moneyStyle);
                createDataRow(overviewSheet, rowNum++, "% thay đổi:", stats.getTodayRevenue().getPercentChange(), dataLabelStyle, dataStyle);
            }
            if (stats.getTodayInvoices() != null) {
                createDataRow(overviewSheet, rowNum++, "Số hóa đơn:", stats.getTodayInvoices().getCount(), dataLabelStyle, dataStyle);
            }
            if (stats.getTodayCustomers() != null) {
                createDataRow(overviewSheet, rowNum++, "Khách hàng:", stats.getTodayCustomers().getCount(), dataLabelStyle, dataStyle);
            }
            if (stats.getActiveTables() != null) {
                Row tableRow = overviewSheet.createRow(rowNum++);
                tableRow.createCell(0).setCellValue("Bàn đang phục vụ:");
                tableRow.getCell(0).setCellStyle(dataLabelStyle);
                Cell tableVal = tableRow.createCell(1);
                tableVal.setCellValue(stats.getActiveTables().getOccupied() + "/" + stats.getActiveTables().getTotal());
                tableVal.setCellStyle(dataStyle);
            }

            for (int i = 0; i < 5; i++) {
                overviewSheet.autoSizeColumn(i);
                overviewSheet.setColumnWidth(i, overviewSheet.getColumnWidth(i) + 1000);
            }

            // ========== Sheet 2: Doanh thu theo ngày ==========
            if (chartData != null && chartData.getLabels() != null && !chartData.getLabels().isEmpty()) {
                Sheet chartSheet = workbook.createSheet("Doanh thu theo ngày");
                int cRow = 0;

                Row cTitle = chartSheet.createRow(cRow++);
                cTitle.createCell(0).setCellValue("DOANH THU THEO NGÀY");
                cTitle.getCell(0).setCellStyle(titleStyle);
                chartSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
                cRow++;

                Row cHeader = chartSheet.createRow(cRow++);
                cHeader.createCell(0).setCellValue("Ngày");
                cHeader.getCell(0).setCellStyle(headerStyle);
                cHeader.createCell(1).setCellValue("Doanh thu");
                cHeader.getCell(1).setCellStyle(headerStyle);

                for (int i = 0; i < chartData.getLabels().size(); i++) {
                    Row cDataRow = chartSheet.createRow(cRow++);
                    Cell labelCell = cDataRow.createCell(0);
                    labelCell.setCellValue(chartData.getLabels().get(i));
                    labelCell.setCellStyle(dateStyle);
                    Cell valCell = cDataRow.createCell(1);
                    valCell.setCellValue(chartData.getData().get(i).doubleValue());
                    valCell.setCellStyle(moneyStyle);
                }

                chartSheet.autoSizeColumn(0);
                chartSheet.autoSizeColumn(1);
                chartSheet.setColumnWidth(0, chartSheet.getColumnWidth(0) + 1000);
                chartSheet.setColumnWidth(1, chartSheet.getColumnWidth(1) + 1000);
            }

            // ========== Sheet 3: Top sản phẩm ==========
            if (topProducts != null && !topProducts.isEmpty()) {
                Sheet prodSheet = workbook.createSheet("Top sản phẩm");
                int pRow = 0;

                Row pTitle = prodSheet.createRow(pRow++);
                pTitle.createCell(0).setCellValue("TOP SẢN PHẨM BÁN CHẠY");
                pTitle.getCell(0).setCellStyle(titleStyle);
                prodSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
                pRow++;

                Row pHeader = prodSheet.createRow(pRow++);
                String[] prodHeaders = {"Hạng", "Tên sản phẩm", "Số lượng", "Doanh thu"};
                for (int i = 0; i < prodHeaders.length; i++) {
                    pHeader.createCell(i).setCellValue(prodHeaders[i]);
                    pHeader.getCell(i).setCellStyle(headerStyle);
                }

                int rank = 1;
                for (TopProductResponse p : topProducts) {
                    Row pDataRow = prodSheet.createRow(pRow++);
                    pDataRow.createCell(0).setCellValue(rank++);
                    pDataRow.getCell(0).setCellStyle(dataStyle);
                    pDataRow.createCell(1).setCellValue(p.getName());
                    pDataRow.getCell(1).setCellStyle(dataStyle);
                    pDataRow.createCell(2).setCellValue(p.getQuantity());
                    pDataRow.getCell(2).setCellStyle(dataStyle);
                    Cell revCell = pDataRow.createCell(3);
                    revCell.setCellValue(p.getRevenue().doubleValue());
                    revCell.setCellStyle(moneyStyle);
                }

                for (int i = 0; i < 4; i++) {
                    prodSheet.autoSizeColumn(i);
                    prodSheet.setColumnWidth(i, prodSheet.getColumnWidth(i) + 1000);
                }
            }

            // ========== Sheet 4: Hóa đơn gần đây ==========
            if (recentInvoices != null && !recentInvoices.isEmpty()) {
                Sheet invSheet = workbook.createSheet("Hóa đơn gần đây");
                int iRow = 0;

                Row iTitle = invSheet.createRow(iRow++);
                iTitle.createCell(0).setCellValue("HÓA ĐƠN GẦN ĐÂY");
                iTitle.getCell(0).setCellStyle(titleStyle);
                invSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
                iRow++;

                Row iHeader = invSheet.createRow(iRow++);
                String[] invHeaders = {"Mã hóa đơn", "Bàn", "Thời gian", "Tổng tiền", "Trạng thái"};
                for (int i = 0; i < invHeaders.length; i++) {
                    iHeader.createCell(i).setCellValue(invHeaders[i]);
                    iHeader.getCell(i).setCellStyle(headerStyle);
                }

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

                for (RecentInvoiceResponse inv : recentInvoices) {
                    Row iDataRow = invSheet.createRow(iRow++);
                    iDataRow.createCell(0).setCellValue(inv.getCode() != null ? inv.getCode() : "");
                    iDataRow.getCell(0).setCellStyle(dataStyle);
                    iDataRow.createCell(1).setCellValue(inv.getTable() != null ? inv.getTable() : "N/A");
                    iDataRow.getCell(1).setCellStyle(dataStyle);
                    Cell timeCell = iDataRow.createCell(2);
                    timeCell.setCellValue(inv.getTime() != null ? dtf.format(inv.getTime()) : "N/A");
                    timeCell.setCellStyle(dateStyle);
                    Cell amountCell = iDataRow.createCell(3);
                    amountCell.setCellValue(inv.getFinalAmount() != null ? inv.getFinalAmount().doubleValue() : 0);
                    amountCell.setCellStyle(moneyStyle);
                    iDataRow.createCell(4).setCellValue(getStatusName(inv.getStatus() != null ? inv.getStatus() : ""));
                    iDataRow.getCell(4).setCellStyle(dataStyle);
                }

                for (int i = 0; i < 5; i++) {
                    invSheet.autoSizeColumn(i);
                    invSheet.setColumnWidth(i, invSheet.getColumnWidth(i) + 1000);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Export Custom Dashboard - tất cả widget gộp vào 1 sheet duy nhất
     */
    public byte[] exportCustomDashboard(
            String dashboardName,
            List<WidgetExportData> widgetDataList
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Header cột: nền xanh, chữ trắng, bold
            CellStyle colHeaderStyle = createHeaderStyle(workbook);

            // Tên widget: bold, không nền
            CellStyle widgetTitleStyle = workbook.createCellStyle();
            Font wFont = workbook.createFont();
            wFont.setBold(true);
            wFont.setFontHeightInPoints((short) 11);
            widgetTitleStyle.setFont(wFont);
            widgetTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Dữ liệu: trắng, border mỏng
            CellStyle dataStyle = createDataStyle(workbook);

            // Số: trắng, border mỏng, canh phải, format #,##0
            CellStyle numStyle = workbook.createCellStyle();
            numStyle.cloneStyleFrom(dataStyle);
            numStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            numStyle.setAlignment(HorizontalAlignment.RIGHT);

            int maxCols = widgetDataList.stream()
                .mapToInt(w -> Math.max(w.getColumns().size(), 1))
                .max().orElse(1);

            Sheet sheet = workbook.createSheet(sanitizeSheetName(dashboardName, 1));

            // Theo dõi độ rộng tối đa mỗi cột (số ký tự)
            int[] maxCharPerCol = new int[maxCols];

            int rowNum = 0;

            // Dòng tiêu đề dashboard
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(22);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(dashboardName + " — Ngày xuất: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            if (maxCols > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, maxCols - 1));
            }

            rowNum++; // dòng trống

            for (WidgetExportData widget : widgetDataList) {
                int numCols = widget.getColumns().size();

                // Tên widget
                Row wTitleRow = sheet.createRow(rowNum++);
                wTitleRow.setHeightInPoints(20);
                Cell wTitleCell = wTitleRow.createCell(0);
                wTitleCell.setCellValue(widget.getQueryName());
                wTitleCell.setCellStyle(widgetTitleStyle);

                // Header cột
                Row headerRow = sheet.createRow(rowNum++);
                headerRow.setHeightInPoints(20);
                for (int c = 0; c < numCols; c++) {
                    String h = widget.getColumns().get(c);
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(h != null ? h : "");
                    cell.setCellStyle(colHeaderStyle);
                    if (h != null) maxCharPerCol[c] = Math.max(maxCharPerCol[c], h.length());
                }

                // Dữ liệu
                if (widget.getRows() == null || widget.getRows().isEmpty()) {
                    Row r = sheet.createRow(rowNum++);
                    Cell c = r.createCell(0);
                    c.setCellValue("(Không có dữ liệu)");
                    c.setCellStyle(dataStyle);
                } else {
                    for (Map<String, Object> row : widget.getRows()) {
                        Row dataRow = sheet.createRow(rowNum++);
                        dataRow.setHeightInPoints(18);
                        int colIdx = 0;
                        for (String key : widget.getColumnKeys()) {
                            Cell cell = dataRow.createCell(colIdx);
                            Object val = row.get(key);
                            if (val == null) {
                                cell.setCellValue("");
                                cell.setCellStyle(dataStyle);
                            } else if (val instanceof Number) {
                                double dv = ((Number) val).doubleValue();
                                cell.setCellValue(dv);
                                cell.setCellStyle(numStyle);
                                maxCharPerCol[colIdx] = Math.max(maxCharPerCol[colIdx],
                                    String.format("%,.0f", dv).length() + 2);
                            } else {
                                String sv = val.toString();
                                cell.setCellValue(sv);
                                cell.setCellStyle(dataStyle);
                                maxCharPerCol[colIdx] = Math.max(maxCharPerCol[colIdx], sv.length());
                            }
                            colIdx++;
                        }
                    }
                }

                rowNum++; // dòng trống giữa các widget
            }

            // Độ rộng cột: 512 POI units/ký tự, tối thiểu 12 ký tự, tối đa ~15cm
            for (int c = 0; c < maxCols; c++) {
                int chars = Math.max(maxCharPerCol[c], 12);
                sheet.setColumnWidth(c, Math.min(chars * 512 + 1024, 25000));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String sanitizeSheetName(String name, int index) {
        if (name == null || name.trim().isEmpty()) {
            return "Sheet " + index;
        }
        String cleaned = name.replaceAll("[\\\\/:*?\\[\\]]", " ").trim();
        if (cleaned.length() > 28) {
            cleaned = cleaned.substring(0, 28);
        }
        return cleaned;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WidgetExportData {
        private String queryName;
        private List<String> columns;      // display names (tiếng Việt)
        private List<String> columnKeys;   // actual keys from query result
        private List<Map<String, Object>> rows;
    }
}
