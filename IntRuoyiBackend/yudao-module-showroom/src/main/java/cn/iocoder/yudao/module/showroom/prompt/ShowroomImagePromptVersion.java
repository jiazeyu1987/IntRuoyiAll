package cn.iocoder.yudao.module.showroom.prompt;

import java.time.LocalDateTime;
import java.util.List;

public record ShowroomImagePromptVersion(Long id,
                                         String sceneCode,
                                         Integer versionNo,
                                         String templateText,
                                         String changeNote,
                                         List<String> placeholderCodes,
                                         Integer useCount,
                                         LocalDateTime createTime,
                                         String creator,
                                         LocalDateTime lastUsedAt) {
}
