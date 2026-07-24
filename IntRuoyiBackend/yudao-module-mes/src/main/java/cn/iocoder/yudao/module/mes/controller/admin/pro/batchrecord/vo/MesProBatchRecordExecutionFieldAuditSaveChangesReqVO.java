package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditSaveChangesReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    @NotNull(message = "workTaskId 不能为空")
    private Long workTaskId;

    @NotBlank(message = "idempotencyKey 不能为空")
    private String idempotencyKey;

    @NotBlank(message = "baseCellValuesHash 不能为空")
    private String baseCellValuesHash;

    @NotNull(message = "baseFieldAuditRevision 不能为空")
    private Long baseFieldAuditRevision;

    @NotBlank(message = "baseFieldAuditHeadHash 不能为空")
    private String baseFieldAuditHeadHash;

    private String fillCarrier;

    private String fillMode;

    @NotBlank(message = "reasonCategory 不能为空")
    private String reasonCategory;

    @NotBlank(message = "reasonText 不能为空")
    private String reasonText;

    @Valid
    @NotNull(message = "signature 不能为空")
    private Signature signature;

    @Valid
    private List<Change> changes;

    @Valid
    private List<AttachmentChange> attachmentChanges;

    @JsonIgnore
    @AssertTrue(message = "changes 与 attachmentChanges 不能同时为空")
    public boolean isHasChangesOrAttachmentChanges() {
        return (changes != null && !changes.isEmpty())
                || (attachmentChanges != null && !attachmentChanges.isEmpty());
    }

    @Data
    @Accessors(chain = true)
    public static class Signature {

        @NotBlank(message = "signature.password 不能为空")
        private String password;

        @Valid
        private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;
    }

    @Data
    @Accessors(chain = true)
    public static class Change {

        @NotBlank(message = "fieldPath 不能为空")
        private String fieldPath;

        @NotBlank(message = "fieldKey 不能为空")
        private String fieldKey;

        @NotNull(message = "rowIndex 不能为空")
        private Integer rowIndex;

        @NotNull(message = "columnIndex 不能为空")
        private Integer columnIndex;

        @NotBlank(message = "valueType 不能为空")
        private String valueType;

        private Object newValueJson;

        @NotNull(message = "newValueDisplay 不能为空")
        private String newValueDisplay;

        private Object expectedOldValueJson;

        private String expectedOldValueHash;

        @JsonIgnore
        @AssertTrue(message = "newValueJson 与 valueType 不匹配")
        public boolean isNewValueJsonMatchesValueType() {
            if (valueType == null || valueType.isBlank()) {
                return true;
            }
            if ("NULL".equals(valueType)) {
                return newValueJson == null;
            }
            return newValueJson != null;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class AttachmentChange {

        @NotNull(message = "workTaskId 不能为空")
        private Long workTaskId;

        @NotBlank(message = "fieldPath 不能为空")
        private String fieldPath;

        @NotBlank(message = "fieldKey 不能为空")
        private String fieldKey;

        private String fieldLabel;

        @NotNull(message = "rowIndex 不能为空")
        private Integer rowIndex;

        @NotNull(message = "columnIndex 不能为空")
        private Integer columnIndex;

        @NotBlank(message = "attachmentType 不能为空")
        private String attachmentType;

        @NotBlank(message = "attachmentAction 不能为空")
        private String attachmentAction;

        @NotBlank(message = "attachmentGroupKey 不能为空")
        private String attachmentGroupKey;

        @NotNull(message = "fileId 不能为空")
        private Long fileId;

        private String fileUrl;

        @NotNull(message = "storageConfigId 不能为空")
        private Long storageConfigId;

        @NotBlank(message = "storagePath 不能为空")
        private String storagePath;

        @NotBlank(message = "fileName 不能为空")
        private String fileName;

        @NotBlank(message = "contentType 不能为空")
        private String contentType;

        @NotNull(message = "fileSize 不能为空")
        private Long fileSize;

        @NotBlank(message = "sha256 不能为空")
        private String sha256;

        private String storageRetentionJson;

        private String expectedPreviousAttachmentHash;
    }
}
