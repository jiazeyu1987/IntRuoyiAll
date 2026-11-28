package cn.iocoder.yudao.module.dcc.registrationcertificate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class RegistrationCertificateMaterialFixtures {
    private static final ConcurrentMap<String, byte[]> PDFS = new ConcurrentHashMap<>();

    private RegistrationCertificateMaterialFixtures() {
    }

    static byte[] pdf(String label) {
        return PDFS.computeIfAbsent(label, key -> {
            try (PDDocument document = new PDDocument(); var output = new ByteArrayOutputStream()) {
                document.addPage(new PDPage());
                document.getDocumentInformation().setTitle(key);
                document.save(output);
                return output.toByteArray();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }).clone();
    }
}
