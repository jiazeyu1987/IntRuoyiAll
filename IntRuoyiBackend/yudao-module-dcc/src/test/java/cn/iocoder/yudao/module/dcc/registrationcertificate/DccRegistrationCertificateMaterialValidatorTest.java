package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateMaterialValidator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRegistrationCertificateMaterialValidatorTest {
    @Test
    void acceptsRealPdfRegardlessOfExtensionCase() {
        assertDoesNotThrow(() -> validate("证书.PDF", RegistrationCertificateMaterialFixtures.pdf("valid")));
    }

    @Test
    void acceptsRealWordDocument() throws Exception {
        try (var word = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            word.createParagraph().createRun().setText("注册证测试材料");
            word.write(output);
            assertDoesNotThrow(() -> validate("证书.docx", output.toByteArray()));
            assertThrows(ServiceException.class, () -> validate("证书.pdf", output.toByteArray()));
            assertThrows(ServiceException.class, () -> validate("证书.doc", output.toByteArray()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg", "jpeg"})
    void acceptsRealImagesAndRejectsRenamedImages(String extension) throws Exception {
        try (var output = new ByteArrayOutputStream()) {
            String format = "jpg".equals(extension) ? "jpeg" : extension;
            assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output));
            assertDoesNotThrow(() -> validate("证书." + extension, output.toByteArray()));
            String wrongExtension = "png".equals(extension) ? "jpg" : "png";
            assertThrows(ServiceException.class, () -> validate("证书." + wrongExtension, output.toByteArray()));
            assertThrows(ServiceException.class, () -> validate("证书.pdf", output.toByteArray()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "doc", "docx", "png", "jpg", "jpeg", "txt", ""})
    void rejectsTextRenamedToSupportedFormat(String extension) {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> validate("伪证书." + extension, "not a certificate".getBytes(StandardCharsets.UTF_8)));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("实际格式与扩展名不符"));
    }

    @Test
    void rejectsPdfHeaderWithoutDocumentStructure() {
        assertThrows(ServiceException.class,
                () -> validate("证书.pdf", "%PDF-1.7\nnot a PDF".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void rejectsSpreadsheetRenamedAsWord() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            workbook.createSheet("not Word");
            workbook.write(output);
            assertThrows(ServiceException.class, () -> validate("证书.docx", output.toByteArray()));
            assertThrows(ServiceException.class, () -> validate("证书.doc", output.toByteArray()));
        }
    }

    @Test
    void rejectsEmptyAndUnsupportedMaterials() {
        assertThrows(ServiceException.class, () -> validate("证书.pdf", new byte[0]));
        assertThrows(ServiceException.class, () -> validate("证书.pdf", null));
        assertThrows(ServiceException.class, () -> validate("证书.txt", RegistrationCertificateMaterialFixtures.pdf("renamed")));
    }

    private static void validate(String name, byte[] bytes) {
        DccRegistrationCertificateMaterialValidator.validate(name, bytes);
    }
}
