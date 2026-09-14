package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.apache.pdfbox.preflight.Format;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

@Component
public class MesProEdhrPdfAValidator {

    public static final String PROFILE = "PDF/A-1b";
    public static final String STATUS_VALID = "VALID";

    public void validateOrThrow(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException(PROFILE + " validation requires non-empty PDF content");
        }
        try {
            ByteArrayDataSource dataSource = new ByteArrayDataSource(new ByteArrayInputStream(pdfBytes));
            PreflightParser parser = new PreflightParser(dataSource);
            parser.parse(Format.PDF_A1B);
            try (PreflightDocument document = parser.getPreflightDocument()) {
                document.validate();
                ValidationResult result = document.getResult();
                if (!result.isValid()) {
                    throw new IllegalStateException(PROFILE + " validation failed: " + formatErrors(result));
                }
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(PROFILE + " validation could not be completed", ex);
        }
    }

    private String formatErrors(ValidationResult result) {
        return result.getErrorsList().stream()
                .map(error -> error.getErrorCode() + ":" + error.getDetails())
                .collect(Collectors.joining("; "));
    }
}
