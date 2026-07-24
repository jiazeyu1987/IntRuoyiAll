package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

final class MesProBatchRecordExternalToolchainPreflight {

    private MesProBatchRecordExternalToolchainPreflight() {
    }

    static String requireCommand(String command, String errorKey) throws IOException {
        String resolvedCommand = StrUtil.blankToDefault(command, "").trim();
        if (resolvedCommand.isEmpty()) {
            throw new IOException(errorKey);
        }
        return resolvedCommand;
    }

    static Path resolveWorkingDirectory(String workingDirectory, String errorKey) throws IOException {
        if (StrUtil.isBlank(workingDirectory)) {
            return null;
        }
        Path path;
        try {
            path = Path.of(workingDirectory.trim());
        } catch (InvalidPathException ex) {
            throw new IOException(errorKey + ":" + workingDirectory, ex);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException(errorKey + ":" + path);
        }
        return path;
    }

    static void requirePositiveTimeout(long timeoutMs, String errorKey) throws IOException {
        if (timeoutMs <= 0) {
            throw new IOException(errorKey + ":" + timeoutMs);
        }
    }
}
