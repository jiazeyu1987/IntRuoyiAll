package cn.iocoder.yudao.module.system.api.workbench;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
/** Neutral public projection. Business action fields belong exclusively to module rows. */
@Data
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
public class TaskSummary {
 private String sourceId, taskKey, businessId, taskType, source, detail, statusLabel;
 @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=WorkbenchLocalDateTimeSerializer.class)
 private LocalDateTime createdAt, dueAt;
 private ActionEnvelope action;
 @JsonIgnore private Integer sourceRank;
 @JsonIgnore private String rowOrderKey;
 public ChunkCursor cursor() { return new ChunkCursor(sourceId, taskType, source, detail, statusLabel, createdAt, dueAt, rowOrderKey); }
 public TaskSummary summary(ActionEnvelope envelope) {
  TaskSummary out = new TaskSummary();
  out.setSourceId(sourceId); out.setTaskKey(taskKey); out.setBusinessId(businessId);
  out.setTaskType(taskType); out.setSource(source); out.setDetail(detail); out.setStatusLabel(statusLabel);
  out.setCreatedAt(createdAt); out.setDueAt(dueAt); out.setRowOrderKey(rowOrderKey);
  out.setAction(envelope); return out;
 }
}
