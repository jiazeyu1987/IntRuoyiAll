package cn.iocoder.yudao.module.infra.service.file;

import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_EXECUTABLE_BLOCKED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FileUploadSecurityPolicyTest {

    private final FileUploadSecurityPolicy policy = new FileUploadSecurityPolicy();

    @Test
    void validate_rejectsExecutableExtensionCaseInsensitiveAndAfterTrailingDots() {
        assertServiceException(() -> policy.validate("installer.exe", "plain".getBytes()),
                FILE_UPLOAD_EXECUTABLE_BLOCKED, "installer.exe");
        assertServiceException(() -> policy.validate("installer.EXE", "plain".getBytes()),
                FILE_UPLOAD_EXECUTABLE_BLOCKED, "installer.EXE");
        assertServiceException(() -> policy.validate("report.pdf.exe. ", "plain".getBytes()),
                FILE_UPLOAD_EXECUTABLE_BLOCKED, "report.pdf.exe");
    }

    @Test
    void validate_rejectsDisguisedPortableExecutableContent() {
        byte[] portableExecutable = new byte[128];
        portableExecutable[0] = 'M';
        portableExecutable[1] = 'Z';
        portableExecutable[0x3c] = 0x40;
        portableExecutable[0x40] = 'P';
        portableExecutable[0x41] = 'E';

        assertServiceException(() -> policy.validate("approved-document.pdf", portableExecutable),
                FILE_UPLOAD_EXECUTABLE_BLOCKED, "approved-document.pdf");
    }

    @Test
    void validate_allowsOrdinaryDocumentContent() {
        assertDoesNotThrow(() -> policy.validate("approved-document.pdf", "%PDF-1.7".getBytes()));
        assertDoesNotThrow(() -> policy.validate("notes.txt", "MZ is text here".getBytes()));
    }
}
