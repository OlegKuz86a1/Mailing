package com.itextpdf.pdfcleanup.autosweep;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;

import java.util.regex.Pattern;

public class RegexBasedCleanupStrategy extends RegexBasedLocationExtractionStrategy implements ICleanupStrategy {
    private Pattern pattern;
    private Color redactionColor;

    public RegexBasedCleanupStrategy(String regex) {
        super(regex);
        this.redactionColor = ColorConstants.WHITE;
        this.pattern = Pattern.compile(regex);
    }

    public RegexBasedCleanupStrategy(Pattern pattern) {
        super(pattern);
        this.redactionColor = ColorConstants.WHITE;
        this.pattern = pattern;
    }

    public Color getRedactionColor(IPdfTextLocation location) {
        return this.redactionColor;
    }

    public com.itextpdf.pdfcleanup.autosweep.RegexBasedCleanupStrategy setRedactionColor(Color color) {
        this.redactionColor = color;
        return this;
    }

    public ICleanupStrategy reset() {
        return (new com.itextpdf.pdfcleanup.autosweep.RegexBasedCleanupStrategy(this.pattern)).setRedactionColor(this.redactionColor);
    }
}
