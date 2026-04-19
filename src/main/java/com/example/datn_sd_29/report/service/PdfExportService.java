package com.example.datn_sd_29.report.service;

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
import java.time.format.DateTimeFormatter;

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
}
