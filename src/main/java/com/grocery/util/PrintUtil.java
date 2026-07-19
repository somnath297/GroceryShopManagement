package com.grocery.util;

import com.grocery.dao.SettingsDAO;
import com.grocery.model.Bill;
import com.grocery.model.BillItem;
import javafx.concurrent.Worker;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * PrintUtil — generates a styled HTML invoice and prints it.
 * Supports both physical printer (via PrinterJob) and PDF (via Microsoft Print to PDF).
 */
public class PrintUtil {

    /**
     * Show a print dialog for the given bill.
     * The invoice is rendered as HTML inside an off-screen WebView,
     * then sent to the system's print dialog (supports physical printer and PDF).
     */
    public static void printBill(Bill bill) {
        String html = generateInvoiceHtml(bill);

        WebView webView = new WebView();
        webView.setPrefSize(794, 1123); // A4 size in pixels at 96dpi
        WebEngine engine = webView.getEngine();
        engine.loadContent(html, "text/html");

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null) {
                    boolean proceed = job.showPrintDialog(null);
                    if (proceed) {
                        engine.print(job);
                        job.endJob();
                    }
                }
            }
        });
    }

    /**
     * Save the bill as an HTML file that the user can open in a browser to print/save as PDF.
     */
    public static void saveAsHtml(Bill bill, Stage ownerStage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Invoice As HTML");
        chooser.setInitialFileName("Invoice_" + bill.getBillNumber() + ".html");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("HTML File", "*.html"));
        File file = chooser.showSaveDialog(ownerStage);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.print(generateInvoiceHtml(bill));
                AlertUtil.showSuccess("Saved", "Invoice saved to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("Save Failed", "Could not save invoice: " + e.getMessage());
            }
        }
    }

    // ── HTML generation ──────────────────────────────────────────────────────

    private static String generateInvoiceHtml(Bill bill) {
        SettingsDAO settings = new SettingsDAO();
        String shopName = safeGet(settings, "shop_name", "Grocery Shop");
        String ownerName = safeGet(settings, "owner_name", "");
        String address  = safeGet(settings, "address", "");
        String phone    = safeGet(settings, "phone", "");
        String gstNo    = safeGet(settings, "gst_number", "");
        String currency = safeGet(settings, "currency", "₹");

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html><head>
            <meta charset="UTF-8">
            <style>
              * { margin:0; padding:0; box-sizing:border-box; }
              body { font-family: Arial, sans-serif; font-size: 13px; color: #222; padding: 20px; }
              .header { text-align: center; margin-bottom: 16px; border-bottom: 2px solid #2e7d32; padding-bottom: 10px; }
              .header h1 { font-size: 22px; color: #2e7d32; }
              .header p  { font-size: 12px; color: #555; }
              .bill-meta { display: flex; justify-content: space-between; margin: 12px 0; font-size: 12px; }
              table { width: 100%; border-collapse: collapse; margin-top: 12px; }
              th { background: #2e7d32; color: white; padding: 8px 6px; text-align: left; }
              td { padding: 7px 6px; border-bottom: 1px solid #e0e0e0; }
              tr:nth-child(even) td { background: #f9f9f9; }
              .totals { margin-top: 16px; text-align: right; }
              .totals table { width: auto; margin-left: auto; }
              .totals td { padding: 4px 10px; }
              .grand-total { font-size: 16px; font-weight: bold; color: #2e7d32; }
              .footer { margin-top: 24px; text-align: center; font-size: 11px; color: #888; border-top: 1px solid #ccc; padding-top: 10px; }
              @media print { body { padding: 0; } }
            </style>
            </head><body>
        """);

        // Header
        sb.append("<div class='header'>");
        sb.append("<h1>").append(escapeHtml(shopName)).append("</h1>");
        if (!ownerName.isBlank()) sb.append("<p>").append(escapeHtml(ownerName)).append("</p>");
        if (!address.isBlank())   sb.append("<p>").append(escapeHtml(address)).append("</p>");
        if (!phone.isBlank())     sb.append("<p>Phone: ").append(escapeHtml(phone)).append("</p>");
        if (!gstNo.isBlank())     sb.append("<p>GST No: ").append(escapeHtml(gstNo)).append("</p>");
        sb.append("</div>");

        // Bill meta
        sb.append("<div class='bill-meta'>");
        sb.append("<div><strong>Bill No:</strong> ").append(bill.getBillNumber()).append("</div>");
        sb.append("<div><strong>Date:</strong> ").append(bill.getBillDate() != null ? bill.getBillDate().substring(0, 16) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).append("</div>");
        sb.append("</div>");
        if (bill.getCustomerName() != null && !bill.getCustomerName().isBlank()) {
            sb.append("<div><strong>Customer:</strong> ").append(escapeHtml(bill.getCustomerName())).append("</div>");
        }
        sb.append("<div><strong>Payment:</strong> ").append(escapeHtml(bill.getPaymentMethod())).append("</div>");

        // Items table
        sb.append("""
            <table>
            <thead><tr>
              <th>#</th><th>Product</th><th>Qty</th><th>Unit</th><th>Rate</th><th>Amount</th>
            </tr></thead><tbody>
        """);
        int sno = 1;
        for (BillItem item : bill.getItems()) {
            sb.append("<tr>")
              .append("<td>").append(sno++).append("</td>")
              .append("<td>").append(escapeHtml(item.getProductName())).append("</td>")
              .append("<td>").append(String.format("%.2f", item.getQuantity())).append("</td>")
              .append("<td>").append(item.getUnit() != null ? item.getUnit() : "").append("</td>")
              .append("<td>").append(currency).append(String.format("%.2f", item.getUnitPrice())).append("</td>")
              .append("<td>").append(currency).append(String.format("%.2f", item.getTotalPrice())).append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table>");

        // Totals
        sb.append("<div class='totals'><table>");
        sb.append("<tr><td>Subtotal:</td><td>").append(currency).append(String.format("%.2f", bill.getSubtotal())).append("</td></tr>");
        if (bill.getDiscount() > 0) {
            sb.append("<tr><td>Discount:</td><td>- ").append(currency).append(String.format("%.2f", bill.getDiscount())).append("</td></tr>");
        }
        if (bill.getGstRate() > 0) {
            sb.append("<tr><td>GST (").append(String.format("%.0f", bill.getGstRate())).append("%):</td><td>")
              .append(currency).append(String.format("%.2f", bill.getGstAmount())).append("</td></tr>");
        }
        sb.append("<tr class='grand-total'><td>TOTAL:</td><td>").append(currency)
          .append(String.format("%.2f", bill.getGrandTotal())).append("</td></tr>");
        sb.append("</table></div>");

        // Footer
        sb.append("<div class='footer'>Thank you for shopping with us! Visit again.</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Print a sales report for a date range.
     * Generates a full HTML page with summary totals and transaction list.
     */
    public static void printReport(
            List<Bill> bills,
            String from, String to,
            double totalSales, double totalProfit, int billCount) {

        String html = generateReportHtml(bills, from, to, totalSales, totalProfit, billCount);

        WebView webView = new WebView();
        webView.setPrefSize(794, 1123);
        WebEngine engine = webView.getEngine();
        engine.loadContent(html, "text/html");

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null) {
                    boolean proceed = job.showPrintDialog(null);
                    if (proceed) {
                        engine.print(job);
                        job.endJob();
                    }
                }
            }
        });
    }

    private static String generateReportHtml(
            List<Bill> bills,
            String from, String to,
            double totalSales, double totalProfit, int billCount) {

        SettingsDAO settings = new SettingsDAO();
        String shopName = safeGet(settings, "shop_name", "Grocery Shop");
        String currency = safeGet(settings, "currency", "₹");

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html><head>
            <meta charset="UTF-8">
            <style>
              * { margin:0; padding:0; box-sizing:border-box; }
              body { font-family: Arial, sans-serif; font-size: 13px; color: #222; padding: 24px; }
              h1 { font-size: 22px; color: #2e7d32; text-align:center; margin-bottom:4px; }
              .subtitle { text-align:center; color:#555; font-size:13px; margin-bottom:16px; }
              .summary { display:flex; gap:20px; margin:16px 0; }
              .summary-box { flex:1; border:1px solid #c8e6c9; border-radius:8px;
                             padding:12px; background:#f1f8e9; text-align:center; }
              .summary-box .label { font-size:11px; color:#555; text-transform:uppercase; }
              .summary-box .value { font-size:20px; font-weight:bold; color:#2e7d32; }
              table { width:100%; border-collapse:collapse; margin-top:16px; }
              th { background:#2e7d32; color:white; padding:9px 8px; text-align:center; font-size:12px; }
              td { padding:7px 8px; border-bottom:1px solid #e0e0e0; text-align:center; font-size:12px; }
              tr:nth-child(even) td { background:#f9f9f9; }
              .footer { margin-top:20px; text-align:center; font-size:11px; color:#888; }
              @media print { body { padding:0; } }
            </style>
            </head><body>
        """);

        // Header
        sb.append("<h1>").append(escapeHtml(shopName)).append("</h1>");
        sb.append("<div class='subtitle'>Sales Report &nbsp;|&nbsp; ")
          .append(from).append(" &nbsp;to&nbsp; ").append(to).append("</div>");

        // Summary boxes
        sb.append("<div class='summary'>");
        sb.append("<div class='summary-box'><div class='label'>Total Sales</div>")
          .append("<div class='value'>").append(currency).append(String.format("%.2f", totalSales)).append("</div></div>");
        sb.append("<div class='summary-box'><div class='label'>Total Profit</div>")
          .append("<div class='value'>").append(currency).append(String.format("%.2f", totalProfit)).append("</div></div>");
        sb.append("<div class='summary-box'><div class='label'>Total Bills</div>")
          .append("<div class='value'>").append(billCount).append("</div></div>");
        sb.append("</div>");

        // Transactions table
        sb.append("""
            <table>
            <thead><tr>
              <th>#</th><th>Bill No</th><th>Customer</th><th>Date</th><th>Payment</th><th>Amount</th>
            </tr></thead><tbody>
        """);

        int sno = 1;
        for (Bill b : bills) {
            String dateStr = b.getBillDate() != null && b.getBillDate().length() >= 10
                ? b.getBillDate().substring(0, 10) : "";
            sb.append("<tr>")
              .append("<td>").append(sno++).append("</td>")
              .append("<td>").append(escapeHtml(b.getBillNumber())).append("</td>")
              .append("<td>").append(escapeHtml(b.getCustomerName())).append("</td>")
              .append("<td>").append(escapeHtml(dateStr)).append("</td>")
              .append("<td>").append(escapeHtml(b.getPaymentMethod())).append("</td>")
              .append("<td>").append(currency).append(String.format("%.2f", b.getGrandTotal())).append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<div class='footer'>Generated on: ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")))
          .append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safeGet(SettingsDAO dao, String key, String def) {
        try { return dao.get(key, def); } catch (Exception e) { return def; }
    }
}

