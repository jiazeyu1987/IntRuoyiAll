package cn.iocoder.yudao.module.dcc.service.file.relations;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/** D owned contracts. FileVersion is an authoritative projection, never a guessed version. */
public final class DccRelationContracts {
    private DccRelationContracts() {}

    public record FileVersion(@JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
                              @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
                              @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
                              @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
                              String fileNumber, String fileName, String versionNo, String status,
                              boolean controlled, boolean pendingEffect, boolean executable) {}
    public record Reference(@JsonFormat(shape=JsonFormat.Shape.STRING) Long id,
                            @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
                            @JsonFormat(shape=JsonFormat.Shape.STRING) Long folderId,
                            @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
                            @JsonFormat(shape=JsonFormat.Shape.STRING) Long selectedControlledFileId,
                            @JsonFormat(shape=JsonFormat.Shape.STRING) Long createdBy) {}
    public record ReferenceView(Reference reference, FileVersion selectedVersion, long referenceProjectCount) {}
    public record ReferenceUsage(@JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
                                 long referenceProjectCount,boolean referenced) {}
    public record ReferenceUsagePage(@JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceControlledFileId,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
            long referenceProjectCount,long visibleReferenceProjectCount,long total,List<ReferenceUsageRow> list,
            boolean detailsRestricted) {}
    public record ReferenceUsageRow(@JsonFormat(shape=JsonFormat.Shape.STRING) Long referenceId,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,String projectName,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long folderId,String folderName,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long selectedControlledFileId,
            String fileNumber,String fileName,String versionNo,String status,
            boolean controlled,boolean pendingEffect,boolean executable,boolean canPreview) {}
    public record RelationChange(@JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceControlledFileId,
                                 @JsonFormat(shape=JsonFormat.Shape.STRING) long rowVersion,
                                 @com.fasterxml.jackson.databind.annotation.JsonSerialize(contentUsing=com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) List<Long> relatedMasterIds) {}
    public record CurrentRelations(@JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceControlledFileId,
                                   @JsonFormat(shape=JsonFormat.Shape.STRING) long rowVersion,List<FileVersion> files) {}
    public record Arrangement(@JsonFormat(shape=JsonFormat.Shape.STRING) Long relatedMasterId,
                              @JsonFormat(shape=JsonFormat.Shape.STRING) Long assigneeUserId,
                              @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
                              @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=DccRelationDeadlineDeserializer.class)
                              LocalDateTime dueAt) {}
    public record ControlledEvent(Long tenantId, Long masterId, Long controlledFileId,
                                  String applicationRound, String eventKey, LocalDateTime controlledAt) {}
    public record Notification(String businessKey, Long recipientUserId, Long sourceControlledFileId,
                               Long relatedMasterId, LocalDateTime dueAt, String fileNumberSnapshot,
                               String versionNoSnapshot) {}
    public record RemediationTask(@JsonFormat(shape=JsonFormat.Shape.STRING) Long id,
                                  @JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceControlledFileId,
                                  @JsonFormat(shape=JsonFormat.Shape.STRING) Long relatedMasterId,
                                  @JsonFormat(shape=JsonFormat.Shape.STRING) Long assigneeUserId,
                                  LocalDateTime dueAt, String status) {}
}
