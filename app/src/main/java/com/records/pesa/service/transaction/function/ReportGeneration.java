package com.records.pesa.service.transaction.function;

import static androidx.core.content.ContextCompat.getDrawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.sqlite.db.SupportSQLiteQuery;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.records.pesa.R;
import com.records.pesa.db.dao.TransactionsDao;
import com.records.pesa.db.models.TransactionCategory;
import com.records.pesa.db.models.TransactionWithCategories;
import com.records.pesa.db.models.UserAccount;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportGeneration {

    public byte[] generateAllTransactionsReport(SupportSQLiteQuery query, TransactionsDao transactionsDao, UserAccount userAccount, String reportType, String startDate, String endDate, Context context) throws ParseException {

        // Fetch user and transactions
        List<TransactionWithCategories> transactions = transactionsDao.getStaticUserTransactions(query);
        List<AllTransactionsReportModel> allTransactionsReportModel = new ArrayList<>();

        // Date formatting
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy");

        Date start = inputFormat.parse(startDate);
        Date end = inputFormat.parse(endDate);

        String formattedStartDate = outputFormat.format(start);
        String formattedEndDate = outputFormat.format(end);

        // Initialize totals
        Double totalIn = 0.0;
        Double totalOut = 0.0;
        Double totalTransactionCost = 0.0;

        // Determine report owner
        String owner;
        if (userAccount.getFname() == null && userAccount.getLname() == null) {
            owner = userAccount.getPhoneNumber();
        } else if (userAccount.getFname() == null) {
            owner = userAccount.getLname();
        } else if (userAccount.getLname() == null) {
            owner = userAccount.getFname();
        } else {
            owner = userAccount.getFname() + " " + userAccount.getLname();
        }

        // Process transactions and build report models
        for (TransactionWithCategories transaction : transactions) {
            List<String> categoryNames = new ArrayList<>();
            String moneyIn = "-";
            String moneyOut = "-";
            String transactionCost = "-";

            if (transaction.getTransaction().getTransactionAmount() > 0) {
                totalIn += transaction.getTransaction().getTransactionAmount();
                moneyIn = "Ksh" + transaction.getTransaction().getTransactionAmount();
            } else if (transaction.getTransaction().getTransactionAmount() < 0) {
                totalOut += Math.abs(transaction.getTransaction().getTransactionAmount());
                totalTransactionCost += Math.abs(transaction.getTransaction().getTransactionCost());
                moneyOut = "Ksh" + Math.abs(transaction.getTransaction().getTransactionAmount());
                transactionCost = "Ksh" + Math.abs(transaction.getTransaction().getTransactionCost());
            }

            if (!transaction.getCategories().isEmpty()) {
                for (TransactionCategory category : transaction.getCategories()) {
                    categoryNames.add(category.getName());
                }
            } else {
                categoryNames.add("-");
            }

            AllTransactionsReportModel model = new AllTransactionsReportModel();
            model.setDatetime(transaction.getTransaction().getDate() + " " + transaction.getTransaction().getTime());
            model.setTransactionType(transaction.getTransaction().getTransactionType());
            model.setCategory(String.join(", ", categoryNames));
            model.setEntity(transaction.getTransaction().getEntity());
            model.setMoneyIn(moneyIn);
            model.setMoneyOut(moneyOut);
            model.setTransactionCost(transactionCost);
            allTransactionsReportModel.add(model);
        }

        if ("csv".equalsIgnoreCase(reportType)) {
            // Generate CSV
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 PrintWriter writer = new PrintWriter(byteArrayOutputStream)) {

                // Write CSV header
                writer.println("Owner:," + owner);
                writer.println("Start Date:," + formattedStartDate);
                writer.println("End Date:," + formattedEndDate);
                writer.println("Total in:,"+ "Ksh"+ String.format("%.2f", totalIn));
                writer.println("Total out:,"+ "Ksh"+ String.format("%.2f", totalOut));
                writer.println();
                writer.println("Report Generated:," + new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss").format(new Date()));
                writer.println();

                writer.println("Date,Time,Transaction Type,Category,Entity,Money In,Money Out,Transaction Cost");

                // Write CSV data
                for (AllTransactionsReportModel model : allTransactionsReportModel) {
                    writer.println(String.join(",",
                            model.getDatetime().split(" ")[0],   // Date
                            model.getDatetime().split(" ")[1],   // Time
                            model.getTransactionType(),
                            model.getCategory(),
                            model.getEntity(),
                            model.getMoneyIn(),
                            model.getMoneyOut(),
                            model.getTransactionCost()
                    ));
                }

                // Write totals at the end
                writer.println(",,,,,Total In: Ksh" + String.format("%.2f", totalIn) +
                        ",Total Out: Ksh" + String.format("%.2f", totalOut) +
                        ",Total Transaction Cost: Ksh" + String.format("%.2f", totalTransactionCost));

                writer.flush();
                return byteArrayOutputStream.toByteArray();

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error generating CSV report", e);
            }
        } else {
            try {
                return createPdf(context, transactions, owner, startDate, endDate);
            } catch (Exception ex) {
                System.err.println("ERROR: "+ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private byte[] createPdf(Context context, List<TransactionWithCategories> transactions, String owner, String startDate, String endDate) {
        double totalIn = 0.0;
        double totalOut = 0.0;
        double totalTransactionCost = 0.0;


        // Iterate through the transactions and accumulate totals
        for (TransactionWithCategories transaction : transactions) {

            // Assuming positive transactionAmount represents money in and negative represents money out
            if (transaction.getTransaction().getTransactionAmount() > 0) {
                totalIn += transaction.getTransaction().getTransactionAmount();
            } else {
                totalOut += Math.abs(transaction.getTransaction().getTransactionAmount());
                totalTransactionCost += Math.abs(transaction.getTransaction().getTransactionCost());
            }

        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument);

        Drawable d = getDrawable(context, R.drawable.cashledger_logo);
        Bitmap bitmap = ((BitmapDrawable)d).getBitmap();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bitmapData = stream.toByteArray();

        ImageData imageData = ImageDataFactory.create(bitmapData);
        Image image = new Image(imageData);
        image.setHeight(180);
        image.setWidth(180);
        image.setFixedPosition(1, 400, 650);


        Text text1 = new Text("Owner: ").setBold().setFontColor(new DeviceRgb(31, 140, 49));
        Text text2= new Text(owner);
        Text text3 = new Text("Start Date: ").setBold().setFontColor(new DeviceRgb(31, 140, 49));
        Text text4 = new Text(startDate);
        Text text5 = new Text("End Date: ").setBold().setFontColor(new DeviceRgb(31, 140, 49));
        Text text6 = new Text(endDate);

        Text text7 = new Text("Total in: ");
        Text text8 = new Text("Ksh" + String.format("%.2f", totalIn)).setFontColor(new DeviceRgb(31, 140, 49));
        Text text9 = new Text("Total out: ");
        Text text10 = new Text("Ksh" + String.format("%.2f", totalOut)).setFontColor(new DeviceRgb(171, 18, 15));
        Text text11 = new Text("Total transaction cost: ");
        Text text12 = new Text("Ksh" + String.format("%.2f", totalTransactionCost)).setFontColor(new DeviceRgb(171, 18, 15));

        Text text13 = new Text(transactions.size()+" row(s)");
        Text text14 = new Text("Report generated on "+new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss").format(new Date()));

        Text spacer = new Text("   ");

        Paragraph paragraph1 = new Paragraph();
        paragraph1
                .add(text1)
                .add(text2);
        Paragraph paragraph2 = new Paragraph();
        paragraph2
                .add(text3)
                .add(text4);
        Paragraph paragraph3 = new Paragraph();
        paragraph3
                .add(text5)
                .add(text6);
        Paragraph paragraph4 = new Paragraph();
        paragraph4
                .add(text7)
                .add(text8)
                .add(spacer)
                .add(text9)
                .add(text10)
                .add(spacer)
                .add(text11)
                .add(text12);
        Paragraph paragraph5 = new Paragraph();
        paragraph5
                .add(text13)
                .add(spacer)
                .add(text14);


        document.add(image);
        document.add(new Paragraph("Cash Ledger").setFontSize(22).setFontColor(new DeviceRgb(31, 140, 49)));
        document.add(new Paragraph("MPESA transactions report"));
        document.add(new Paragraph());
        document.add(paragraph1);
        document.add(paragraph2);
        document.add(paragraph3);
        document.add(new Paragraph());
        document.add(paragraph4);
        document.add(new Paragraph());
        document.add(paragraph5);
        document.add(new Paragraph());



        float[] columnWidth = {110f, 100f, 120f, 100f, 100f, 100f, 120f};
        Table table = new Table(columnWidth);
        DeviceRgb headerColor = new DeviceRgb(31, 140, 49);

        addTableHeader(table, headerColor);

        List<String> categoryNames = new ArrayList<>();
        for (TransactionWithCategories transaction : transactions) {
            categoryNames.clear(); // Reuse the list

            String moneyIn = "-";
            String moneyOut = "-";
            String transactionCost = "-";

            if (transaction.getTransaction().getTransactionAmount() > 0) {
                totalIn += transaction.getTransaction().getTransactionAmount();
                moneyIn = "Ksh" + transaction.getTransaction().getTransactionAmount();
            } else if (transaction.getTransaction().getTransactionAmount() < 0) {
                totalOut += Math.abs(transaction.getTransaction().getTransactionAmount());
                totalTransactionCost += Math.abs(transaction.getTransaction().getTransactionCost());
                moneyOut = "Ksh" + Math.abs(transaction.getTransaction().getTransactionAmount());
                transactionCost = "Ksh" + Math.abs(transaction.getTransaction().getTransactionCost());
            }

            if (!transaction.getCategories().isEmpty()) {
                for (TransactionCategory category : transaction.getCategories()) {
                    categoryNames.add(category.getName());
                }
            } else {
                categoryNames.add("-");
            }

            addTransactionRow(table, transaction, moneyIn, moneyOut, transactionCost, categoryNames);
        }

        document.add(table);
        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    private void addTableHeader(Table table, DeviceRgb headerColor) {
        String[] headers = {"Datetime", "Entity", "Transaction type", "Category", "Money in", "Money out", "Transaction cost"};
        for (String header : headers) {
            table.addCell(new Cell()
                    .setBackgroundColor(headerColor)
                    .add(new Paragraph(header).setBold().setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.WHITE).setHorizontalAlignment(HorizontalAlignment.CENTER))
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));
        }
    }

    private void addTransactionRow(Table table, TransactionWithCategories transaction, String moneyIn, String moneyOut, String transactionCost, List<String> categoryNames) {
        // Combine date and time into one string for easier formatting
        String dateTime = transaction.getTransaction().getDate() + " " + transaction.getTransaction().getTime();

        // Add each cell with proper alignment
        table.addCell(new Cell()
                .add(new Paragraph(dateTime)
                        .setTextAlignment(TextAlignment.CENTER))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(transaction.getTransaction().getEntity())
                        .setTextAlignment(TextAlignment.CENTER))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(transaction.getTransaction().getTransactionType())
                        .setTextAlignment(TextAlignment.CENTER))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(String.join(", ", categoryNames))
                        .setTextAlignment(TextAlignment.CENTER))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(moneyIn)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(31, 140, 49)))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(moneyOut)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(171, 18, 15)))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addCell(new Cell()
                .add(new Paragraph(transactionCost)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(171, 18, 15)))
                        .setBold()
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
    }


}