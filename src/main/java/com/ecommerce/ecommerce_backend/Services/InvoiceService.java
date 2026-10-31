package com.ecommerce.ecommerce_backend.Services;

import com.ecommerce.ecommerce_backend.Repositories.OrderRepository;
import com.ecommerce.ecommerce_backend.model.Order;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public ByteArrayResource generateInvoicePdf(Long orderId) {
        // 1. Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // 2. Header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Paragraph title = new Paragraph("INVOICE", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // 3. Order Details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Order Number: " + (order.getOrderNumber() != null ? order.getOrderNumber() : "N/A"), normalFont));
            document.add(new Paragraph("Order ID: " + order.getId(), normalFont));

            String date = order.getPlacedAt() != null
                    ? order.getPlacedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                    : "N/A";
            document.add(new Paragraph("Date: " + date, normalFont));
            document.add(new Paragraph(" "));

            // 4. Customer Details
            String username = "Customer";
            if (order.getUser() != null && order.getUser().getUsername() != null) {
                username = order.getUser().getUsername();
            }
            String address = order.getShippingAddress() != null ? order.getShippingAddress() : "No address provided";

            document.add(new Paragraph("Customer: " + username, normalFont));
            document.add(new Paragraph("Shipping Address: " + address, normalFont));
            document.add(new Paragraph(" "));

            // 5. Total Amount
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph total = new Paragraph("Total Amount: $" + (order.getTotalAmount() != null ? order.getTotalAmount() : "0.00"), boldFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Thank you for your business!", normalFont));

            // 6. Close the document
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }

        return new ByteArrayResource(out.toByteArray());
    }
}