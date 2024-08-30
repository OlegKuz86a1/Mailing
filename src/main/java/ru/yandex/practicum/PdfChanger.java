package ru.yandex.practicum;


import ch.qos.logback.core.pattern.color.WhiteCompositeConverter;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.pdfcleanup.PdfCleaner;
import com.itextpdf.pdfcleanup.autosweep.CompositeCleanupStrategy;
import com.itextpdf.pdfcleanup.autosweep.ICleanupStrategy;
import com.itextpdf.pdfcleanup.autosweep.RegexBasedCleanupStrategy;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.texts.PdfTextReplaceOptions;
import com.spire.pdf.texts.PdfTextReplacer;
import com.spire.pdf.texts.ReplaceActionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.EnumSet;

@Slf4j
public class PdfChanger {

    public static void main(String[] args) throws SQLException, IOException {
        // Create a PdfDocument object
        com.spire.pdf.PdfDocument doc = new com.spire.pdf.PdfDocument();

        // Load a PDF file
        doc.loadFromFile("src/main/resources/Коммерческое предложение.pdf");

        // Create a PdfTextReplaceOptions object
        PdfTextReplaceOptions textReplaceOptions = new PdfTextReplaceOptions();

        // Specify the options for text replacement
        textReplaceOptions.setReplaceType(EnumSet.of(ReplaceActionType.IgnoreCase));
        textReplaceOptions.setReplaceType(EnumSet.of(ReplaceActionType.WholeWord));

        // Get a specific page
        PdfPageBase page = doc.getPages().get(0);

        // Create a PdfTextReplacer object based on the page
        PdfTextReplacer textReplacer = new PdfTextReplacer(page);

        // Set the replace options
        textReplacer.setOptions(textReplaceOptions);

        // Replace all instances of target text with new text
        textReplacer.replaceAllText("$number от date$", "173 от 17.03.2024");
        textReplacer.replaceAllText("$postfix name$", "ый Олег Юрьевич");
        textReplacer.replaceAllText("Evaluation Warning : The document was created with Spire.PDF for Java.", "");

        // Save the document to a different PDF file
        doc.saveToFile("src/main/resources/temp/Коммерческое предложение_temp.pdf");

        // Dispose resources
        doc.dispose();

        try {
            InputStream inputPdf = new FileInputStream("src/main/resources/temp/Коммерческое предложение_temp.pdf");
            OutputStream outputPdf = new FileOutputStream("src/main/resources/temp/Коммерческое предложение.pdf");
            ICleanupStrategy strategy = new RegexBasedCleanupStrategy("Evaluation Warning : The document was created with Spire.PDF for Java.");
            PdfCleaner.autoSweepCleanUp(inputPdf, outputPdf, strategy);
            inputPdf.close();
            outputPdf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Watermark removed successfully.");
    }
}
