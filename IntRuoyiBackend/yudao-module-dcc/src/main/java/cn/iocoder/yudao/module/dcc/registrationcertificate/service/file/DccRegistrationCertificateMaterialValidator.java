package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Validates material bytes before any registration-certificate submission is persisted. */
public final class DccRegistrationCertificateMaterialValidator {

    private DccRegistrationCertificateMaterialValidator() {
    }

    public static void validate(String originalName, byte[] content) {
        String name = originalName == null ? "" : originalName.trim();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (content == null || content.length == 0) {
            throw invalidMaterial(name, null);
        }
        try {
            boolean valid = switch (extension) {
                case "pdf" -> isPdf(content);
                case "docx" -> isDocx(content);
                case "doc" -> isDoc(content);
                case "png", "jpg", "jpeg" -> isImage(extension, content);
                default -> false;
            };
            if (!valid) {
                throw invalidMaterial(name, null);
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw invalidMaterial(name, exception);
        }
    }

    private static boolean isPdf(byte[] content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            return document.getNumberOfPages() > 0;
        }
    }

    private static boolean isDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            return document.getDocument().getBody() != null;
        }
    }

    private static boolean isDoc(byte[] content) throws IOException {
        try (POIFSFileSystem document = new POIFSFileSystem(new ByteArrayInputStream(content))) {
            var root = document.getRoot();
            if (!root.hasEntry("WordDocument")) {
                return false;
            }
            // Word's binary header identifies the Word stream and the matching table stream.
            try (var word = root.createDocumentInputStream("WordDocument")) {
                byte[] header = word.readNBytes(32);
                if (header.length < 32 || (header[0] & 0xff) != 0xec || (header[1] & 0xff) != 0xa5) {
                    return false;
                }
                return root.hasEntry((header[11] & 0x02) == 0 ? "0Table" : "1Table");
            }
        }
    }

    private static boolean isImage(String extension, byte[] content) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String expected = "jpg".equals(extension) ? "jpeg" : extension;
                if (!expected.equals(format)) {
                    return false;
                }
                reader.setInput(input);
                return reader.read(0) != null;
            } finally {
                reader.dispose();
            }
        }
    }

    private static ServiceException invalidMaterial(String name, Throwable cause) {
        ServiceException exception = invalidParamException(
                "材料文件“" + name + "”的实际格式与扩展名不符或内容损坏；请上传有效的 PDF、Word（DOC/DOCX）或图片（PNG/JPG/JPEG）");
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }
}
