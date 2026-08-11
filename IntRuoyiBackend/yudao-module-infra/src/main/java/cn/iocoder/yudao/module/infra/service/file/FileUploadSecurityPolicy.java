package cn.iocoder.yudao.module.infra.service.file;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_EXECUTABLE_BLOCKED;

@Component
public class FileUploadSecurityPolicy {

    private static final String EXECUTABLE_EXTENSION = ".exe";
    private static final int DOS_HEADER_PE_OFFSET = 0x3c;

    public void validate(String originalFilename, byte[] content) {
        if (hasExecutableExtension(originalFilename) || hasPortableExecutableSignature(content)) {
            throw exception(FILE_UPLOAD_EXECUTABLE_BLOCKED, normalizedDisplayName(originalFilename));
        }
    }

    private boolean hasExecutableExtension(String originalFilename) {
        String filename = normalizedDisplayName(originalFilename).toLowerCase(Locale.ROOT);
        return filename.endsWith(EXECUTABLE_EXTENSION);
    }

    private boolean hasPortableExecutableSignature(byte[] content) {
        if (content == null || content.length < DOS_HEADER_PE_OFFSET + Integer.BYTES
                || content[0] != 'M' || content[1] != 'Z') {
            return false;
        }
        int peOffsetValue = (content[DOS_HEADER_PE_OFFSET] & 0xff)
                | ((content[DOS_HEADER_PE_OFFSET + 1] & 0xff) << 8)
                | ((content[DOS_HEADER_PE_OFFSET + 2] & 0xff) << 16)
                | ((content[DOS_HEADER_PE_OFFSET + 3] & 0xff) << 24);
        long peOffset = Integer.toUnsignedLong(peOffsetValue);
        return peOffset + 4 <= content.length
                && content[(int) peOffset] == 'P'
                && content[(int) peOffset + 1] == 'E'
                && content[(int) peOffset + 2] == 0
                && content[(int) peOffset + 3] == 0;
    }

    private String normalizedDisplayName(String originalFilename) {
        if (originalFilename == null) {
            return "未命名文件";
        }
        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFKC).replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        int streamSeparator = normalized.indexOf(':');
        if (streamSeparator >= 0) {
            normalized = normalized.substring(0, streamSeparator);
        }
        int end = normalized.length();
        while (end > 0 && (normalized.charAt(end - 1) == '.' || Character.isWhitespace(normalized.charAt(end - 1)))) {
            end--;
        }
        normalized = normalized.substring(0, end);
        return normalized.isEmpty() ? "未命名文件" : normalized;
    }
}
