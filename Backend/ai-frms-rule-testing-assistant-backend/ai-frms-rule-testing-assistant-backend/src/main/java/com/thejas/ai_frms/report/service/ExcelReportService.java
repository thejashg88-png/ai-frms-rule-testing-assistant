package com.thejas.ai_frms.report.service;

import com.thejas.ai_frms.report.dto.ReportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelReportService {

    private final String outputDirectory;

    public ExcelReportService(@Value("${app.report.output-dir:reports}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public ReportResponse generateExcelReport(String reportName, List<String> lines, String generatedBy) {
        try {
            Path outputPath = Path.of(outputDirectory);
            Files.createDirectories(outputPath);

            String fileName = buildFileName(reportName, "csv");
            Path filePath = outputPath.resolve(fileName);

            String csvContent = buildCsvContent(reportName, lines);
            Files.writeString(filePath, csvContent);

            ReportResponse response = new ReportResponse();
            response.setReportName(reportName);
            response.setReportType("EXCEL");
            response.setFileName(fileName);
            response.setFilePath(filePath.toAbsolutePath().toString());
            response.setStatus("SUCCESS");
            response.setMessage("Excel/CSV report generated successfully");
            response.setGeneratedBy(generatedBy);
            response.setGeneratedAt(LocalDateTime.now());

            return response;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate Excel report", exception);
        }
    }

    private String buildCsvContent(String reportName, List<String> lines) {
        StringBuilder csv = new StringBuilder();

        csv.append("Report Name,").append(escapeCsv(reportName)).append("\n");
        csv.append("Generated At,").append(escapeCsv(LocalDateTime.now().toString())).append("\n");
        csv.append("\n");
        csv.append("Sl No,Details\n");

        if (lines != null) {
            int index = 1;

            for (String line : lines) {
                csv.append(index++)
                        .append(",")
                        .append(escapeCsv(line))
                        .append("\n");
            }
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escapedValue = value.replace("\"", "\"\"");

        if (escapedValue.contains(",") || escapedValue.contains("\n") || escapedValue.contains("\"")) {
            return "\"" + escapedValue + "\"";
        }

        return escapedValue;
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