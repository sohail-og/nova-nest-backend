package com.novanest.service;

import com.novanest.model.Order;
import com.novanest.model.OrderItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReportExcelService {

    public byte[] generateDailyReportExcel(List<Order> todayOrders, List<OrderItem> todayOrderItems) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Business Report");

            // Fonts
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);

            // Premium custom Gold (#CAA750) color for headers
            byte[] goldRgb = new byte[]{(byte) 202, (byte) 167, (byte) 80};
            XSSFColor goldColor = new XSSFColor(goldRgb, null);

            // Cell Styles
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(goldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("[$₹-409]#,##0.00")); 
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle boldLabelStyle = workbook.createCellStyle();
            boldLabelStyle.setFont(boldFont);
            boldLabelStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(format.getFormat("[$₹-409]#,##0.00"));
            boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Columns headers
            String[] columns = {
                "Order ID", "Customer Name", "Email", "Product Name", 
                "Category", "Quantity", "Price", "Payment Method", 
                "Order Status", "Order Date", "Revenue"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal totalRevenueSum = BigDecimal.ZERO;
            Set<String> uniqueOrders = new HashSet<>();
            Set<Integer> uniqueCustomers = new HashSet<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (OrderItem item : todayOrderItems) {
                Row row = sheet.createRow(rowIdx++);
                Order order = item.getOrder();

                uniqueOrders.add(order.getOrderId());
                if (order.getUser() != null) {
                    uniqueCustomers.add(order.getUser().getId());
                }

                // Order ID
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(order.getOrderId());
                cell0.setCellStyle(dataStyle);

                // Customer Name
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(order.getUser() != null ? order.getUser().getUsername() : "N/A");
                cell1.setCellStyle(dataStyle);

                // Email
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(order.getUser() != null ? order.getUser().getEmail() : "N/A");
                cell2.setCellStyle(dataStyle);

                // Product Name
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(item.getProduct() != null ? item.getProduct().getName() : "N/A");
                cell3.setCellStyle(dataStyle);

                // Category
                Cell cell4 = row.createCell(4);
                String catName = (item.getProduct() != null && item.getProduct().getCategory() != null)
                        ? item.getProduct().getCategory().getCategoryName()
                        : "N/A";
                cell4.setCellValue(catName);
                cell4.setCellStyle(dataStyle);

                // Quantity
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(item.getQuantity());
                cell5.setCellStyle(dataStyle);

                // Price
                Cell cell6 = row.createCell(6);
                double priceVal = item.getPricePerUnit() != null ? item.getPricePerUnit().doubleValue() : 0.0;
                cell6.setCellValue(priceVal);
                cell6.setCellStyle(currencyStyle);

                // Payment Method
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD");
                cell7.setCellStyle(dataStyle);

                // Order Status
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(order.getStatus() != null ? order.getStatus().toString() : "PENDING");
                cell8.setCellStyle(dataStyle);

                // Order Date
                Cell cell9 = row.createCell(9);
                String formattedDate = order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "";
                cell9.setCellValue(formattedDate);
                cell9.setCellStyle(dataStyle);

                // Revenue
                Cell cell10 = row.createCell(10);
                BigDecimal revenue = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                cell10.setCellValue(revenue.doubleValue());
                cell10.setCellStyle(currencyStyle);

                totalRevenueSum = totalRevenueSum.add(revenue);
            }

            // Calculations
            int totalOrders = uniqueOrders.size();
            int totalCustomersCount = uniqueCustomers.size();
            double avgOrderValue = totalOrders > 0 
                ? totalRevenueSum.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

            // Space
            rowIdx += 2;

            // Bottom Rows:
            // Total Orders
            Row rowOrders = sheet.createRow(rowIdx++);
            Cell cellOrdersLbl = rowOrders.createCell(0);
            cellOrdersLbl.setCellValue("Total Orders");
            cellOrdersLbl.setCellStyle(boldLabelStyle);
            Cell cellOrdersVal = rowOrders.createCell(1);
            cellOrdersVal.setCellValue(totalOrders);
            cellOrdersVal.setCellStyle(boldLabelStyle);

            // Total Revenue
            Row rowRev = sheet.createRow(rowIdx++);
            Cell cellRevLbl = rowRev.createCell(0);
            cellRevLbl.setCellValue("Total Revenue");
            cellRevLbl.setCellStyle(boldLabelStyle);
            Cell cellRevVal = rowRev.createCell(1);
            cellRevVal.setCellValue(totalRevenueSum.doubleValue());
            cellRevVal.setCellStyle(boldCurrencyStyle);

            // Total Customers
            Row rowCust = sheet.createRow(rowIdx++);
            Cell cellCustLbl = rowCust.createCell(0);
            cellCustLbl.setCellValue("Total Customers");
            cellCustLbl.setCellStyle(boldLabelStyle);
            Cell cellCustVal = rowCust.createCell(1);
            cellCustVal.setCellValue(totalCustomersCount);
            cellCustVal.setCellStyle(boldLabelStyle);

            // Average Order Value
            Row rowAvg = sheet.createRow(rowIdx++);
            Cell cellAvgLbl = rowAvg.createCell(0);
            cellAvgLbl.setCellValue("Average Order Value");
            cellAvgLbl.setCellStyle(boldLabelStyle);
            Cell cellAvgVal = rowAvg.createCell(1);
            cellAvgVal.setCellValue(avgOrderValue);
            cellAvgVal.setCellStyle(boldCurrencyStyle);

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
