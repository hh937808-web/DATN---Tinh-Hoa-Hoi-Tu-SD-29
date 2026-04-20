package com.example.datn_sd_29.report.service;

import com.example.datn_sd_29.dashboard.dto.DashboardStatsResponse;
import com.example.datn_sd_29.dashboard.dto.RecentInvoiceResponse;
import com.example.datn_sd_29.dashboard.dto.RevenueChartResponse;
import com.example.datn_sd_29.dashboard.dto.TopProductResponse;
import com.example.datn_sd_29.report.dto.ProductReportResponse;
import com.example.datn_sd_29.report.dto.RevenueReportResponse;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    public byte[] exportRevenueReport(RevenueReportResponse report) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Use Arial Unicode MS or fallback to built-in font with Identity-H encoding
            PdfFont font;
            PdfFont boldFont;
            try {
                // Try to use system Arial font with Unicode support
                font = PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", PdfEncodings.IDENTITY_H);
                boldFont = PdfFontFactory.createFont("c:/windows/fonts/arialbd.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                // Fallback to Helvetica if Arial not found
                font = PdfFontFactory.createFont("Helvetica");
                boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            }
            
            // Title
            Paragraph title = new Paragraph("BÁO CÁO DOANH THU")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);
            
            // Date range
            Paragraph dateRange = new Paragraph(
                String.format("Từ ngày %s đến ngày %s",
                    report.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    report.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            )
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(dateRange);
            
            // Summary section
            Paragraph summaryHeader = new Paragraph("TỔNG QUAN")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(summaryHeader);
            
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
            
            addSummaryRow(summaryTable, "Tổng doanh thu:", formatMoney(report.getTotalRevenue()), font, boldFont);
            addSummaryRow(summaryTable, "Tổng hóa đơn:", String.valueOf(report.getTotalInvoices()), font, boldFont);
            addSummaryRow(summaryTable, "Giá trị TB/đơn:", formatMoney(report.getAverageOrderValue()), font, boldFont);
            addSummaryRow(summaryTable, "Doanh thu tiền mặt:", formatMoney(report.getCashRevenue()), font, boldFont);
            addSummaryRow(summaryTable, "Doanh thu chuyển khoản:", formatMoney(report.getTransferRevenue()), font, boldFont);
            
            document.add(summaryTable);
            
            // Payment methods section
            Paragraph pmHeader = new Paragraph("PHƯƠNG THỨC THANH TOÁN")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(pmHeader);
            
            Table pmTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
            
            // Header row
            addHeaderCell(pmTable, "Phương thức", boldFont);
            addHeaderCell(pmTable, "Số đơn", boldFont);
            addHeaderCell(pmTable, "Doanh thu", boldFont);
            addHeaderCell(pmTable, "Tỷ lệ %", boldFont);
            
            // Data rows
            for (RevenueReportResponse.PaymentMethodBreakdown pm : report.getPaymentMethods()) {
                pmTable.addCell(new Cell().add(new Paragraph(getPaymentMethodName(pm.getMethod())).setFont(font)));
                pmTable.addCell(new Cell().add(new Paragraph(String.valueOf(pm.getCount())).setFont(font)));
                pmTable.addCell(new Cell().add(new Paragraph(formatMoney(pm.getAmount())).setFont(font)));
                pmTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", pm.getPercentage())).setFont(font)));
            }
            
            document.add(pmTable);
            
            // Invoice channels section
            Paragraph icHeader = new Paragraph("LOẠI HÓA ĐƠN")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(icHeader);
            
            Table icTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
            
            // Header row
            addHeaderCell(icTable, "Loại", boldFont);
            addHeaderCell(icTable, "Số đơn", boldFont);
            addHeaderCell(icTable, "Doanh thu", boldFont);
            addHeaderCell(icTable, "Tỷ lệ %", boldFont);
            
            // Data rows
            if (report.getInvoiceChannels() != null) {
                for (RevenueReportResponse.InvoiceChannelBreakdown ic : report.getInvoiceChannels()) {
                    icTable.addCell(new Cell().add(new Paragraph(getInvoiceChannelName(ic.getChannel())).setFont(font)));
                    icTable.addCell(new Cell().add(new Paragraph(String.valueOf(ic.getCount())).setFont(font)));
                    icTable.addCell(new Cell().add(new Paragraph(formatMoney(ic.getAmount())).setFont(font)));
                    icTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", ic.getPercentage())).setFont(font)));
                }
            }
            
            document.add(icTable);
            
            // Daily revenue section
            Paragraph dailyHeader = new Paragraph("DOANH THU THEO NGÀY")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(dailyHeader);
            
            Table dailyTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1}))
                .setWidth(UnitValue.createPercentValue(100));
            
            // Header row
            addHeaderCell(dailyTable, "Ngày", boldFont);
            addHeaderCell(dailyTable, "Doanh thu", boldFont);
            addHeaderCell(dailyTable, "Số hóa đơn", boldFont);
            
            // Data rows
            for (RevenueReportResponse.DailyRevenue daily : report.getDailyRevenues()) {
                dailyTable.addCell(new Cell().add(new Paragraph(
                    daily.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ).setFont(font)));
                dailyTable.addCell(new Cell().add(new Paragraph(formatMoney(daily.getRevenue())).setFont(font)));
                dailyTable.addCell(new Cell().add(new Paragraph(String.valueOf(daily.getInvoiceCount())).setFont(font)));
            }
            
            document.add(dailyTable);
        }
        
        return out.toByteArray();
    }

    public byte[] exportProductReport(ProductReportResponse report) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Use Arial Unicode MS or fallback to built-in font with Identity-H encoding
            PdfFont font;
            PdfFont boldFont;
            try {
                // Try to use system Arial font with Unicode support
                font = PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", PdfEncodings.IDENTITY_H);
                boldFont = PdfFontFactory.createFont("c:/windows/fonts/arialbd.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                // Fallback to Helvetica if Arial not found
                font = PdfFontFactory.createFont("Helvetica");
                boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            }
            
            // Title
            Paragraph title = new Paragraph("BÁO CÁO MÓN ĂN BÁN CHẠY")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);
            
            // Date range
            Paragraph dateRange = new Paragraph(
                String.format("Từ ngày %s đến ngày %s",
                    report.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    report.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            )
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(dateRange);
            
            // Top by revenue section
            Paragraph revHeader = new Paragraph("TOP MÓN ĂN THEO DOANH THU")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(revHeader);
            
            Table revTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 1, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
            
            // Header row
            addHeaderCell(revTable, "Hạng", boldFont);
            addHeaderCell(revTable, "Tên món", boldFont);
            addHeaderCell(revTable, "Danh mục", boldFont);
            addHeaderCell(revTable, "Số lượng", boldFont);
            addHeaderCell(revTable, "Doanh thu", boldFont);
            
            // Data rows
            int rank = 1;
            for (ProductReportResponse.ProductSales product : report.getTopByRevenue()) {
                revTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++)).setFont(font)));
                revTable.addCell(new Cell().add(new Paragraph(product.getProductName()).setFont(font)));
                revTable.addCell(new Cell().add(new Paragraph(getCategoryName(product.getCategory())).setFont(font)));
                revTable.addCell(new Cell().add(new Paragraph(String.valueOf(product.getQuantitySold())).setFont(font)));
                revTable.addCell(new Cell().add(new Paragraph(formatMoney(product.getRevenue())).setFont(font)));
            }
            
            document.add(revTable);
            
            // Top by quantity section
            Paragraph qtyHeader = new Paragraph("TOP MÓN ĂN THEO SỐ LƯỢNG")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
            document.add(qtyHeader);
            
            Table qtyTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 1, 2}))
                .setWidth(UnitValue.createPercentValue(100));
            
            // Header row
            addHeaderCell(qtyTable, "Hạng", boldFont);
            addHeaderCell(qtyTable, "Tên món", boldFont);
            addHeaderCell(qtyTable, "Danh mục", boldFont);
            addHeaderCell(qtyTable, "Số lượng", boldFont);
            addHeaderCell(qtyTable, "Doanh thu", boldFont);
            
            // Data rows
            rank = 1;
            for (ProductReportResponse.ProductSales product : report.getTopByQuantity()) {
                qtyTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++)).setFont(font)));
                qtyTable.addCell(new Cell().add(new Paragraph(product.getProductName()).setFont(font)));
                qtyTable.addCell(new Cell().add(new Paragraph(getCategoryName(product.getCategory())).setFont(font)));
                qtyTable.addCell(new Cell().add(new Paragraph(String.valueOf(product.getQuantitySold())).setFont(font)));
                qtyTable.addCell(new Cell().add(new Paragraph(formatMoney(product.getRevenue())).setFont(font)));
            }
            
            document.add(qtyTable);
        }
        
        return out.toByteArray();
    }

    public byte[] exportDashboard(
            DashboardStatsResponse stats,
            RevenueChartResponse chartData,
            List<TopProductResponse> topProducts,
            List<RecentInvoiceResponse> recentInvoices,
            LocalDate startDate,
            LocalDate endDate
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            PdfFont font;
            PdfFont boldFont;
            try {
                font = PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", PdfEncodings.IDENTITY_H);
                boldFont = PdfFontFactory.createFont("c:/windows/fonts/arialbd.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                font = PdfFontFactory.createFont("Helvetica");
                boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            }

            // Title
            document.add(new Paragraph("BÁO CÁO DASHBOARD")
                .setFont(boldFont).setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

            document.add(new Paragraph(
                String.format("Từ ngày %s đến ngày %s",
                    startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .setFont(font).setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Section 1: Thống kê tổng quan
            document.add(new Paragraph("THỐNG KÊ TỔNG QUAN")
                .setFont(boldFont).setFontSize(14).setMarginBottom(10));

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

            if (stats.getTodayRevenue() != null) {
                addSummaryRow(summaryTable, "Doanh thu:", formatMoney(stats.getTodayRevenue().getAmount()), font, boldFont);
                addSummaryRow(summaryTable, "% thay đổi so với kỳ trước:",
                    String.format("%.1f%%", stats.getTodayRevenue().getPercentChange()), font, boldFont);
            }
            if (stats.getTodayInvoices() != null) {
                addSummaryRow(summaryTable, "Số hóa đơn:",
                    String.valueOf(stats.getTodayInvoices().getCount()), font, boldFont);
            }
            if (stats.getTodayCustomers() != null) {
                addSummaryRow(summaryTable, "Khách hàng:",
                    String.valueOf(stats.getTodayCustomers().getCount()), font, boldFont);
            }
            if (stats.getActiveTables() != null) {
                addSummaryRow(summaryTable, "Bàn đang phục vụ:",
                    stats.getActiveTables().getOccupied() + "/" + stats.getActiveTables().getTotal(), font, boldFont);
            }
            document.add(summaryTable);

            // Section 2: Doanh thu theo ngày
            if (chartData != null && chartData.getLabels() != null && !chartData.getLabels().isEmpty()) {
                document.add(new Paragraph("DOANH THU THEO NGÀY")
                    .setFont(boldFont).setFontSize(14).setMarginBottom(10));

                Table chartTable = new Table(UnitValue.createPercentArray(new float[]{2, 2}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

                addHeaderCell(chartTable, "Ngày", boldFont);
                addHeaderCell(chartTable, "Doanh thu", boldFont);

                for (int i = 0; i < chartData.getLabels().size(); i++) {
                    chartTable.addCell(new Cell().add(new Paragraph(chartData.getLabels().get(i)).setFont(font)));
                    chartTable.addCell(new Cell().add(new Paragraph(formatMoney(chartData.getData().get(i))).setFont(font)));
                }
                document.add(chartTable);
            }

            // Section 3: Top sản phẩm
            if (topProducts != null && !topProducts.isEmpty()) {
                document.add(new Paragraph("TOP SẢN PHẨM BÁN CHẠY")
                    .setFont(boldFont).setFontSize(14).setMarginBottom(10));

                Table prodTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 2}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

                addHeaderCell(prodTable, "Hạng", boldFont);
                addHeaderCell(prodTable, "Tên sản phẩm", boldFont);
                addHeaderCell(prodTable, "Số lượng", boldFont);
                addHeaderCell(prodTable, "Doanh thu", boldFont);

                int rank = 1;
                for (TopProductResponse p : topProducts) {
                    prodTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++)).setFont(font)));
                    prodTable.addCell(new Cell().add(new Paragraph(p.getName()).setFont(font)));
                    prodTable.addCell(new Cell().add(new Paragraph(String.valueOf(p.getQuantity())).setFont(font)));
                    prodTable.addCell(new Cell().add(new Paragraph(formatMoney(p.getRevenue())).setFont(font)));
                }
                document.add(prodTable);
            }

            // Section 4: Hóa đơn gần đây
            if (recentInvoices != null && !recentInvoices.isEmpty()) {
                document.add(new Paragraph("HÓA ĐƠN GẦN ĐÂY")
                    .setFont(boldFont).setFontSize(14).setMarginBottom(10));

                Table invTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2, 2, 1.5f}))
                    .setWidth(UnitValue.createPercentValue(100));

                addHeaderCell(invTable, "Mã HĐ", boldFont);
                addHeaderCell(invTable, "Bàn", boldFont);
                addHeaderCell(invTable, "Thời gian", boldFont);
                addHeaderCell(invTable, "Tổng tiền", boldFont);
                addHeaderCell(invTable, "Trạng thái", boldFont);

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

                for (RecentInvoiceResponse inv : recentInvoices) {
                    invTable.addCell(new Cell().add(new Paragraph(
                        inv.getCode() != null ? inv.getCode() : "").setFont(font).setFontSize(9)));
                    invTable.addCell(new Cell().add(new Paragraph(
                        inv.getTable() != null ? inv.getTable() : "N/A").setFont(font).setFontSize(9)));
                    invTable.addCell(new Cell().add(new Paragraph(
                        inv.getTime() != null ? dtf.format(inv.getTime()) : "N/A").setFont(font).setFontSize(9)));
                    invTable.addCell(new Cell().add(new Paragraph(
                        inv.getFinalAmount() != null ? formatMoney(inv.getFinalAmount()) : "0đ").setFont(font).setFontSize(9)));
                    invTable.addCell(new Cell().add(new Paragraph(
                        getDashboardStatusName(inv.getStatus())).setFont(font).setFontSize(9)));
                }
                document.add(invTable);
            }
        }

        return out.toByteArray();
    }

    private String getDashboardStatusName(String status) {
        if (status == null) return "";
        switch (status) {
            case "PAID": return "Đã thanh toán";
            case "IN_PROGRESS": return "Đang phục vụ";
            case "RESERVED": return "Đã đặt bàn";
            case "CANCELLED": return "Đã hủy";
            case "NO_SHOW": return "Không đến";
            default: return status;
        }
    }

    private void addSummaryRow(Table table, String label, String value, PdfFont font, PdfFont boldFont) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)));
    }

    private void addHeaderCell(Table table, String text, PdfFont boldFont) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFont(boldFont))
            .setBackgroundColor(new DeviceRgb(230, 230, 230))
            .setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(cell);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0fđ", amount.doubleValue());
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

    /**
     * Export Custom Dashboard - mỗi widget = 1 section trong PDF
     */
    public byte[] exportCustomDashboard(
            String dashboardName,
            List<ExcelExportService.WidgetExportData> widgetDataList
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            PdfFont font;
            PdfFont boldFont;
            try {
                font = PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", PdfEncodings.IDENTITY_H);
                boldFont = PdfFontFactory.createFont("c:/windows/fonts/arialbd.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                font = PdfFontFactory.createFont("Helvetica");
                boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            }

            // Title
            document.add(new Paragraph(dashboardName != null ? dashboardName : "Custom Dashboard")
                .setFont(boldFont).setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

            document.add(new Paragraph("Xuất ngày: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(font).setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(24));

            for (int idx = 0; idx < widgetDataList.size(); idx++) {
                ExcelExportService.WidgetExportData widget = widgetDataList.get(idx);

                // Widget title
                document.add(new Paragraph((idx + 1) + ". " + widget.getQueryName())
                    .setFont(boldFont).setFontSize(13).setMarginBottom(8));

                if (widget.getColumns().isEmpty() || widget.getRows().isEmpty()) {
                    document.add(new Paragraph("Không có dữ liệu")
                        .setFont(font).setFontSize(10).setMarginBottom(16));
                    continue;
                }

                // Calculate column widths - equal distribution
                int colCount = widget.getColumns().size();
                float[] colWidths = new float[colCount];
                for (int i = 0; i < colCount; i++) {
                    colWidths[i] = 1f;
                }

                Table table = new Table(UnitValue.createPercentArray(colWidths))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(24);

                // Header
                for (String colName : widget.getColumns()) {
                    addHeaderCell(table, colName, boldFont);
                }

                // Data rows
                for (Map<String, Object> row : widget.getRows()) {
                    for (String key : widget.getColumnKeys()) {
                        Object value = row.get(key);
                        String displayValue;
                        if (value == null) {
                            displayValue = "";
                        } else if (value instanceof Number) {
                            displayValue = String.format("%,.0f", ((Number) value).doubleValue());
                        } else {
                            displayValue = value.toString();
                        }
                        table.addCell(new Cell().add(
                            new Paragraph(displayValue).setFont(font).setFontSize(9)));
                    }
                }

                document.add(table);
            }
        }

        return out.toByteArray();
    }
}
