package com.thejas.ai_frms.report.service;

import com.thejas.ai_frms.report.dto.ReportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PdfReportService {

    private final String outputDirectory;

    public PdfReportService(@Value("${app.report.output-dir:reports}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public ReportResponse generatePdfReport(String reportName, List<String> lines, String generatedBy) {
        try {
            Path outputPath = Path.of(outputDirectory);
            Files.createDirectories(outputPath);

            String fileName = buildFileName(reportName, "pdf");
            Path filePath = outputPath.resolve(fileName);

            byte[] pdfBytes = buildSimplePdf(reportName, lines);
            Files.write(filePath, pdfBytes);

            ReportResponse response = new ReportResponse();
            response.setReportName(reportName);
            response.setReportType("PDF");
            response.setFileName(fileName);
            response.setFilePath(filePath.toAbsolutePath().toString());
            response.setStatus("SUCCESS");
            response.setMessage("PDF report generated successfully");
            response.setGeneratedBy(generatedBy);
            response.setGeneratedAt(LocalDateTime.now());

            return response;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate PDF report", exception);
        }
    }

    private byte[] buildSimplePdf(String title, List<String> lines) {
        List<String> printableLines = new ArrayList<>();
        printableLines.add(title);
        printableLines.add("Generated At: " + LocalDateTime.now());
        printableLines.add("");

        if (lines != null) {
            printableLines.addAll(lines);
        }

        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 12 Tf\n");
        content.append("50 760 Td\n");

        int maxLines = Math.min(printableLines.size(), 38);

        for (int i = 0; i < maxLines; i++) {
            String line = escapePdfText(printableLines.get(i));
            content.append("(").append(line).append(") Tj\n");
            content.append("0 -18 Td\n");
        }

        content.append("ET\n");

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.UTF_8);

        List<byte[]> objects = new ArrayList<>();

        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.UTF_8));
        objects.add("""
                3 0 obj
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]
                /Resources << /Font << /F1 4 0 R >> >>
                /Contents 5 0 R >>
                endobj
                """.getBytes(StandardCharsets.UTF_8));
        objects.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

        String contentObjectHeader = "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n";
        String contentObjectFooter = "endstream\nendobj\n";

        byte[] contentObject = concat(
                contentObjectHeader.getBytes(StandardCharsets.UTF_8),
                contentBytes,
                contentObjectFooter.getBytes(StandardCharsets.UTF_8)
        );

        objects.add(contentObject);

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        int offset = pdf.toString().getBytes(StandardCharsets.UTF_8).length;

        for (byte[] object : objects) {
            offsets.add(offset);
            offset += object.length;
            pdf.append(new String(object, StandardCharsets.UTF_8));
        }

        int xrefOffset = offset;

        pdf.append("xref\n");
        pdf.append("0 6\n");
        pdf.append("0000000000 65535 f \n");

        for (Integer objectOffset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", objectOffset));
        }

        pdf.append("trailer\n");
        pdf.append("<< /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF");

        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] concat(byte[] first, byte[] second, byte[] third) {
        byte[] result = new byte[first.length + second.length + third.length];

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        System.arraycopy(third, 0, result, first.length + second.length, third.length);

        return result;
    }

    private String escapePdfText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String buildFileName(String reportName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String cleanName = reportName == null ? "report" : reportName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        return cleanName + "_" + timestamp + "." + extension;
    }
}