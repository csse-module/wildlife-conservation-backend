package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.config.ReportProperties;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportPdfRenderer {
    private static final int LINES_PER_PAGE = 48;
    private static final float FONT_SIZE = 11;
    private final ReportProperties properties;

    public byte[] render(ReportEntity report) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = font(document);
            List<String> lines = wrap(content(report), font);
            for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
                writePage(document, font, lines.subList(start, Math.min(lines.size(), start + LINES_PER_PAGE)));
            }
            document.getDocumentInformation().setTitle("Wildlife conservation report " + report.getId());
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_RENDERING_FAILED",
                    "The report PDF could not be generated. Check the configured report font.");
        }
    }

    private PDFont font(PDDocument document) throws IOException {
        if (properties.getFontPath().isBlank()) {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        try (var input = Files.newInputStream(Path.of(properties.getFontPath()))) {
            return PDType0Font.load(document, input);
        }
    }

    private List<String> content(ReportEntity report) {
        List<String> lines = new ArrayList<>();
        lines.add("Wildlife conservation report");
        lines.add("Report: " + report.getId());
        lines.add("Park: " + report.getParkId());
        lines.add("Type: " + report.getReportType());
        lines.add("Period: " + report.getFrom() + " to " + report.getTo());
        lines.add("Generated: " + report.getGeneratedAt());
        lines.add("Data available: " + report.getSnapshot().isDataAvailable());
        var snapshot = report.getSnapshot();
        for (var section : report.getSections()) {
            lines.add("");
            lines.add(section.name().replace('_', ' '));
            switch (section) {
                case INCIDENT_STATISTICS -> {
                    lines.add("Total incidents: " + snapshot.getTotalIncidents());
                    snapshot.getIncidentTypeCounts().forEach(row -> lines.add(row.getType() + ": " + row.getCount()));
                }
                case INCIDENT_TRENDS -> snapshot.getDailyIncidentCounts().forEach(row -> lines.add(row.getDate() + ": " + row.getCount()));
                case HOTSPOT_SUMMARY -> {
                    lines.add("Potential hotspot threshold: at least 3 incidents.");
                    snapshot.getAreaCounts().forEach(row -> lines.add(row.getAreaName() + " (" + row.getAreaId() + "): "
                            + row.getIncidentCount() + (row.isPotentialHotspot() ? " - Potential hotspot" : "")));
                }
                case PATROL_COVERAGE -> {
                    var coverage = snapshot.getPatrolCoverage();
                    lines.add("Route completion coverage: " + coverage.getCompletedRouteCount() + " / " + coverage.getAssignedRouteCount());
                    lines.add(coverage.getCoveragePercent() == null ? "No scheduled routes in this period." : coverage.getCoveragePercent() + "%");
                    lines.add("Basis: unique assigned routes completed; this is not geographic area coverage.");
                }
                case CONFLICT_SUMMARY -> {
                    lines.add("Community reports: " + snapshot.getCommunityReportCount());
                    lines.add("Resolved alerts: " + snapshot.getResolvedAlertCount());
                }
            }
        }
        return lines;
    }

    private List<String> wrap(List<String> paragraphs, PDFont font) throws IOException {
        List<String> result = new ArrayList<>();
        for (String paragraph : paragraphs) {
            StringBuilder line = new StringBuilder();
            for (int index = 0; index < paragraph.length(); index++) {
                String candidate = line.toString() + paragraph.charAt(index);
                if (font.getStringWidth(candidate) * FONT_SIZE / 1000 > 510) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(paragraph.charAt(index));
            }
            result.add(line.toString());
        }
        return result;
    }

    private void writePage(PDDocument document, PDFont font, List<String> lines) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(font, FONT_SIZE);
            stream.newLineAtOffset(48, 744);
            for (String line : lines) {
                stream.showText(line);
                stream.newLineAtOffset(0, -14);
            }
            stream.endText();
        }
    }
}
