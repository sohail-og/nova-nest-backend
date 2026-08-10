package com.novanest.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.novanest.model.Order;
import com.novanest.model.OrderItem;
import com.novanest.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);

    private static final Color GOLD = new Color(202, 167, 80); // #CAA750
    private static final Color DARK = new Color(18, 18, 20); // #121214
    private static final Color BORDER_GREY = new Color(226, 226, 228);

    @org.springframework.beans.factory.annotation.Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    public byte[] generateInvoicePdf(Order order, List<OrderItem> items) {
        long startTime = System.currentTimeMillis();
        String customerName = order.getShippingFullName() != null ? order.getShippingFullName() : (order.getUser() != null ? order.getUser().getUsername() : "N/A");
        String invoiceId = "INV-" + order.getOrderId().replace("ORD-", "");
        
        log.info("[PDF GENERATION] PDF generation started for Invoice ID: {}, Order ID: {}, Customer: {}", 
                 invoiceId, order.getOrderId(), customerName);

        Document document = new Document(PageSize.A4, 36, 36, 48, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. HEADER LOGO & INVOICE DETAILS TABLE
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3f, 2f});

            // Logo cell (Left)
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            Paragraph brandText = new Paragraph("N O V A N E S T", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, GOLD));
            brandText.setSpacingAfter(4);
            logoCell.addElement(brandText);
            Paragraph brandSubtext = new Paragraph("LUXURY HOME LIVING ACCENTS", FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.GRAY));
            logoCell.addElement(brandSubtext);
            headerTable.addCell(logoCell);

            // Invoice Meta cell (Right)
            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Paragraph title = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, DARK));
            title.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(title);

            String formattedDate = order.getCreatedAt() != null 
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                : "";

            invoiceId = "INV-" + order.getOrderId().replace("ORD-", "");

            Paragraph invoiceMeta = new Paragraph(
                "Invoice Number: " + invoiceId + "\n" +
                "Order Number: " + order.getOrderId() + "\n" +
                "Order Date: " + formattedDate,
                FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY)
            );
            invoiceMeta.setAlignment(Element.ALIGN_RIGHT);
            invoiceMeta.setSpacingBefore(5);
            metaCell.addElement(invoiceMeta);
            headerTable.addCell(metaCell);

            document.add(headerTable);

            // Thin gold divider line
            document.add(new Paragraph(" "));
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderWidth(1.5f);
            lineCell.setBorderColor(GOLD);
            divider.addCell(lineCell);
            document.add(divider);
            document.add(new Paragraph(" "));

            // 2. CUSTOMER & BILLING INFORMATION
            PdfPTable billingTable = new PdfPTable(2);
            billingTable.setWidthPercentage(100);
            billingTable.setWidths(new float[]{1.1f, 0.9f});

            // Customer details cell
            PdfPCell customerCell = new PdfPCell();
            customerCell.setBorder(Rectangle.NO_BORDER);
            customerCell.addElement(new Paragraph("BILL TO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, GOLD)));
            
            if (order.getUser() != null) {
                log.info("[PDF GENERATION] Customer loaded: {}", order.getUser().getUsername());
            } else {
                log.info("[PDF GENERATION] Customer loaded: N/A (User is null)");
            }
            if (order.getShippingFullName() != null) {
                log.info("[PDF GENERATION] Address loaded from Order");
            } else {
                log.info("[PDF GENERATION] Address loaded: Not Provided (No Address linked to Order)");
            }

            String name = "N/A";
            String phone = "N/A";
            String email = "N/A";
            StringBuilder addressBuilder = new StringBuilder();

            if (order.getShippingFullName() != null || order.getShippingHouseNo() != null) {
                name = order.getShippingFullName() != null ? order.getShippingFullName() : "N/A";
                phone = order.getShippingPhone() != null ? order.getShippingPhone() : "N/A";
                email = order.getUser() != null ? order.getUser().getEmail() : "N/A"; // Assuming email from User or AddressDto (if it was copied). Order doesn't have shippingEmail.

                if (order.getShippingHouseNo() != null && !order.getShippingHouseNo().isEmpty()) {
                    addressBuilder.append(order.getShippingHouseNo()).append("\n");
                }
                if (order.getShippingStreet() != null && !order.getShippingStreet().isEmpty()) {
                    addressBuilder.append(order.getShippingStreet()).append("\n");
                }
                if (order.getShippingArea() != null && !order.getShippingArea().isEmpty()) {
                    addressBuilder.append(order.getShippingArea()).append("\n");
                }
                if (order.getShippingCity() != null && !order.getShippingCity().isEmpty()) {
                    addressBuilder.append(order.getShippingCity()).append("\n");
                }
                if (order.getShippingDistrict() != null && !order.getShippingDistrict().isEmpty()) {
                    addressBuilder.append(order.getShippingDistrict()).append("\n");
                }
                if (order.getShippingState() != null && !order.getShippingState().isEmpty()) {
                    addressBuilder.append(order.getShippingState()).append("\n");
                }
                if (order.getShippingCountry() != null && !order.getShippingCountry().isEmpty()) {
                    addressBuilder.append(order.getShippingCountry()).append("\n");
                }
                if (order.getShippingPincode() != null && !order.getShippingPincode().isEmpty()) {
                    addressBuilder.append(order.getShippingPincode());
                }
            } else {
                // Fallback to user saved profile address
                if (order.getUser() != null) {
                    name = order.getUser().getUsername();
                    email = order.getUser().getEmail();
                    phone = order.getUser().getPhone();
                    
                    String houseNo = order.getUser().getHouseNo() != null ? order.getUser().getHouseNo() : "";
                    String street = order.getUser().getStreet() != null ? order.getUser().getStreet() : "";
                    String area = order.getUser().getArea() != null ? order.getUser().getArea() : "";
                    String city = order.getUser().getCity() != null ? order.getUser().getCity() : "";
                    String district = order.getUser().getDistrict() != null ? order.getUser().getDistrict() : "";
                    String state = order.getUser().getState() != null ? order.getUser().getState() : "";
                    String country = order.getUser().getCountry() != null ? order.getUser().getCountry() : "";
                    String pincode = order.getUser().getPincode() != null ? order.getUser().getPincode() : "";
                    
                    if (!houseNo.isEmpty()) addressBuilder.append(houseNo).append("\n");
                    if (!street.isEmpty()) addressBuilder.append(street).append("\n");
                    if (!area.isEmpty()) addressBuilder.append(area).append("\n");
                    if (!city.isEmpty()) addressBuilder.append(city).append("\n");
                    if (!district.isEmpty()) addressBuilder.append(district).append("\n");
                    if (!state.isEmpty()) addressBuilder.append(state).append("\n");
                    if (!country.isEmpty()) addressBuilder.append(country).append("\n");
                    if (!pincode.isEmpty()) addressBuilder.append(pincode);
                }
            }

            if (addressBuilder.length() == 0) {
                addressBuilder.append("Address:\nNot Provided");
            }

            Paragraph customerInfo = new Paragraph(
                name + "\n\n" +
                "Email:\n" + email + "\n\n" +
                "Phone:\n" + phone + "\n\n" +
                "Shipping Address:\n\n" + addressBuilder.toString(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, DARK)
            );
            customerInfo.setSpacingBefore(4);
            customerCell.addElement(customerInfo);
            billingTable.addCell(customerCell);

            // Payment metadata cell
            PdfPCell paymentCell = new PdfPCell();
            paymentCell.setBorder(Rectangle.NO_BORDER);
            paymentCell.addElement(new Paragraph("TRANSACTION DETAIL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, GOLD)));
            
            String payMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : "Cash On Delivery";
            String orderStatus = order.getStatus() != null ? order.getStatus().toString() : "PENDING";

            Paragraph paymentInfo = new Paragraph(
                "Payment Method: " + payMethod + "\n" +
                "Payment Status: " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "PENDING") + "\n" +
                "Order Status: " + orderStatus,
                FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, DARK)
            );
            paymentInfo.setSpacingBefore(4);
            paymentCell.addElement(paymentInfo);
            billingTable.addCell(paymentCell);

            document.add(billingTable);
            document.add(new Paragraph(" "));

            // 3. PRODUCT TABLE
            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{1.2f, 2.8f, 1.5f, 0.8f, 1.7f, 1.7f});
            itemsTable.setSpacingBefore(10);
            itemsTable.setSpacingAfter(10);

            // Headers
            String[] headers = {"Accent", "Product Name", "Category", "Qty", "Price", "Total"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE);
            for (String headerText : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(headerText, headerFont));
                cell.setBackgroundColor(DARK);
                cell.setBorderColor(BORDER_GREY);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(cell);
            }

            Font itemFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, DARK);

            for (OrderItem item : items) {
                PdfPCell imgCell = new PdfPCell();
                imgCell.setBorderColor(BORDER_GREY);
                imgCell.setPadding(4);
                imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                boolean imgLoaded = false;
                if (item.getProduct() != null && item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().trim().isEmpty()) {
                    try {
                        String imgUrl = item.getProduct().getImageUrl();
                        if (imgUrl.startsWith("/uploads/")) {
                            imgUrl = backendUrl + imgUrl;
                        }
                        log.info("[PDF GENERATION] Loading image for product: {}, URL: {}", item.getProduct().getName(), imgUrl);
                        URL url = new URL(imgUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(1500);
                        connection.setReadTimeout(1500);
                        connection.setRequestMethod("GET");
                        
                        try (InputStream inStream = connection.getInputStream()) {
                            byte[] imgBytes = inStream.readAllBytes();
                            Image productImg = Image.getInstance(imgBytes);
                            productImg.scaleToFit(35, 45);
                            imgCell.addElement(productImg);
                            imgLoaded = true;
                            log.info("[PDF GENERATION] Image loaded successfully for product: {}", item.getProduct().getName());
                        }
                    } catch (Exception e) {
                        log.warn("[PDF GENERATION] Failed to load image for product: {}. Fallback to placeholder. Error: {}", 
                                 item.getProduct().getName(), e.getMessage());
                    }
                }

                if (!imgLoaded) {
                    Paragraph placeholder = new Paragraph("No Image", FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.GRAY));
                    placeholder.setAlignment(Element.ALIGN_CENTER);
                    imgCell.addElement(placeholder);
                }
                itemsTable.addCell(imgCell);

                // Cell 2: Product Name
                String prodName = item.getProduct() != null ? item.getProduct().getName() : "Unknown Accent";
                PdfPCell nameCell = new PdfPCell(new Paragraph(prodName, itemFont));
                nameCell.setBorderColor(BORDER_GREY);
                nameCell.setPadding(8);
                nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(nameCell);

                // Cell 3: Category
                String catName = (item.getProduct() != null && item.getProduct().getCategory() != null) 
                    ? item.getProduct().getCategory().getCategoryName() 
                    : "Luxury Item";
                PdfPCell catCell = new PdfPCell(new Paragraph(catName, itemFont));
                catCell.setBorderColor(BORDER_GREY);
                catCell.setPadding(8);
                catCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                catCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(catCell);

                // Cell 4: Quantity
                PdfPCell qtyCell = new PdfPCell(new Paragraph(String.valueOf(item.getQuantity()), itemFont));
                qtyCell.setBorderColor(BORDER_GREY);
                qtyCell.setPadding(8);
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(qtyCell);

                // Cell 5: Price
                BigDecimal price = item.getPricePerUnit() != null ? item.getPricePerUnit() : BigDecimal.ZERO;
                PdfPCell priceCell = new PdfPCell(new Paragraph("₹ " + price.setScale(2, RoundingMode.HALF_UP), itemFont));
                priceCell.setBorderColor(BORDER_GREY);
                priceCell.setPadding(8);
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(priceCell);

                // Cell 6: Total
                BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                PdfPCell totalCell = new PdfPCell(new Paragraph("₹ " + itemTotal.setScale(2, RoundingMode.HALF_UP), itemFont));
                totalCell.setBorderColor(BORDER_GREY);
                totalCell.setPadding(8);
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                itemsTable.addCell(totalCell);
            }

            document.add(itemsTable);

            // 4. PRICING BREAKDOWN & TOTALS (Full Width Table)
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{3f, 1f});
            
            BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal shippingFee = BigDecimal.valueOf(250); // standard shipping charge
            BigDecimal subtotalVal = grandTotal.compareTo(shippingFee) >= 0 ? grandTotal.subtract(shippingFee) : BigDecimal.ZERO;
            BigDecimal subtotalExclTax = subtotalVal.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = subtotalVal.subtract(subtotalExclTax);

            Font totalLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, DARK);
            Font totalValueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, DARK);

            totalsTable.addCell(createNoBorderCell("Subtotal (Excl. Tax)", totalLabelFont, Element.ALIGN_RIGHT));
            totalsTable.addCell(createNoBorderCell("₹ " + subtotalExclTax, totalValueFont, Element.ALIGN_RIGHT));

            totalsTable.addCell(createNoBorderCell("GST (18% Included)", totalLabelFont, Element.ALIGN_RIGHT));
            totalsTable.addCell(createNoBorderCell("₹ " + taxAmount, totalValueFont, Element.ALIGN_RIGHT));

            totalsTable.addCell(createNoBorderCell("Shipping & Handling", totalLabelFont, Element.ALIGN_RIGHT));
            totalsTable.addCell(createNoBorderCell("₹ " + shippingFee, totalValueFont, Element.ALIGN_RIGHT));

            PdfPCell gtLabel = new PdfPCell(new Paragraph("Grand Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.WHITE)));
            gtLabel.setBackgroundColor(GOLD);
            gtLabel.setPadding(6);
            gtLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            gtLabel.setBorder(Rectangle.NO_BORDER);
            totalsTable.addCell(gtLabel);

            PdfPCell gtValue = new PdfPCell(new Paragraph("₹ " + grandTotal.setScale(2, RoundingMode.HALF_UP), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.WHITE)));
            gtValue.setBackgroundColor(GOLD);
            gtValue.setPadding(6);
            gtValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            gtValue.setBorder(Rectangle.NO_BORDER);
            totalsTable.addCell(gtValue);

            document.add(totalsTable);
            document.add(new Paragraph(" "));
            
            // 5. CENTERED HIGH-RES QR CODE
            PdfPTable qrTable = new PdfPTable(1);
            qrTable.setWidthPercentage(100);
            
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            try {
                // High-resolution QR Code: 250x250 pixels
                ByteArrayOutputStream qrStream = new ByteArrayOutputStream();
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                
                String qrContent = "Order ID: " + order.getOrderId() + "\nInvoice ID: " + invoiceId;
                
                BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 250, 250);
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
                
                Image qrImage = Image.getInstance(qrStream.toByteArray());
                qrImage.scaleToFit(110, 110);
                qrImage.setAlignment(Image.ALIGN_CENTER);
                
                Paragraph qrLabel = new Paragraph("VERIFY TRANSACTION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, GOLD));
                qrLabel.setAlignment(Element.ALIGN_CENTER);
                qrLabel.setSpacingAfter(4);
                
                qrCell.addElement(qrLabel);
                qrCell.addElement(qrImage);
                log.info("[PDF GENERATION] QR generated successfully for Order ID: {}", order.getOrderId());
            } catch (Exception e) {
                log.warn("[PDF GENERATION] QR code generation failed for Order ID: {}. Printing placeholder text.", order.getOrderId(), e);
                Paragraph qrLabel = new Paragraph("VERIFY TRANSACTION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, GOLD));
                qrLabel.setAlignment(Element.ALIGN_CENTER);
                qrLabel.setSpacingAfter(4);
                qrCell.addElement(qrLabel);
                
                Paragraph unavailable = new Paragraph("QR Code Unavailable", FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.RED));
                unavailable.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(unavailable);
            }
            
            qrTable.addCell(qrCell);
            document.add(qrTable);
            document.add(new Paragraph(" "));

            // 6. INVOICE FOOTER NOTE
            Paragraph thankYou = new Paragraph("Thank you for shopping with NovaNest.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Font.ITALIC, GOLD));
            thankYou.setAlignment(Element.ALIGN_CENTER);
            thankYou.setSpacingBefore(15);
            document.add(thankYou);

            document.close();
        } catch (Exception e) {
            log.error("[PDF GENERATION] Failed to generate PDF for Order ID: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        } finally {
            try {
                if (document.isOpen()) {
                    document.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                out.flush();
                out.close();
            } catch (Exception e) {
                // ignore
            }
        }

        byte[] pdfBytes = out.toByteArray();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("[PDF GENERATION] PDF finalized for Invoice ID: {}, Order ID: {}, File Size: {} bytes, Generation Time: {} ms", 
                 invoiceId, order.getOrderId(), pdfBytes.length, duration);

        return pdfBytes;
    }

    private PdfPCell createNoBorderCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
