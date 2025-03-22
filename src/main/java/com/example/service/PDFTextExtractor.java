package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class PDFTextExtractor {

    private final Tesseract tesseract;

    public PDFTextExtractor() {
        // Initialize Tesseract for OCR
        tesseract = new Tesseract();
        // You may need to set the Tesseract data path if it's not in the system path
        // tesseract.setDatapath("/path/to/tessdata");
        tesseract.setLanguage("eng");
    }

    public String extractText(File pdfFile) throws IOException {
        StringBuilder extractedText = new StringBuilder();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            // Try direct text extraction first
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }

            // If direct extraction yields no text, try OCR
            System.out.println("Direct text extraction yielded no results. Falling back to OCR.");

            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                try {
                    String pageText = tesseract.doOCR(image);
                    extractedText.append(pageText).append("\n");
                } catch (TesseractException e) {
                    System.err.println("OCR failed for page " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return extractedText.toString().trim();
    }
}