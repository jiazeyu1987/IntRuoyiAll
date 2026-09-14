package cn.iocoder.yudao.module.infra.service.file;

import java.time.LocalDateTime;

public record NasRecursiveSkippedDirectory(
        String path,
        String reason,
        LocalDateTime skippedAt
) {
}
