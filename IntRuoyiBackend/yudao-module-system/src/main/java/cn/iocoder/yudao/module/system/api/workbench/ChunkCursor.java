package cn.iocoder.yudao.module.system.api.workbench;
import java.time.LocalDateTime;
public record ChunkCursor(String sourceId, String taskType, String source, String detail,
 String statusLabel, LocalDateTime createdAt, LocalDateTime dueAt, String rowOrderKey) { }
