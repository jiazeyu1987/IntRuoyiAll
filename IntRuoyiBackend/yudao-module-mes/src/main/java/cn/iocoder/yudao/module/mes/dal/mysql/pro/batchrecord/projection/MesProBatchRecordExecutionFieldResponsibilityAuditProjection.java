package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.projection;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProBatchRecordExecutionFieldResponsibilityAuditProjection {

    private Long auditItemId;
    private Long auditBatchId;
    private Long executionId;
    private Long tenantId;
    private Long fieldAuditRevision;
    private Integer batchItemIndex;
    private String fieldPath;
    private String fieldKey;
    private String fieldLabel;
    private Integer rowIndex;
    private Integer columnIndex;
    private String component;
    private String valueType;
    private String oldValueJson;
    private String oldValueDisplay;
    private String oldValueHash;
    private String newValueJson;
    private String newValueDisplay;
    private String newValueHash;
    private String reasonCategory;
    private String reasonText;
    private Long actorId;
    private String actorName;
    private Long signatureId;
    private String signatureProjectionHash;
    private String previousHash;
    private String auditHash;
    private String beforeCellValuesHash;
    private String afterCellValuesHash;
    private String executionSnapshotHash;
    private LocalDateTime changedAt;

    public static MesProBatchRecordExecutionFieldResponsibilityAuditProjection from(
            MesProBatchRecordExecutionFieldAuditItemDO item) {
        return MesProBatchRecordExecutionFieldResponsibilityAuditProjection.builder()
                .auditItemId(item.getId())
                .auditBatchId(item.getAuditBatchId())
                .executionId(item.getExecutionId())
                .tenantId(item.getTenantId())
                .fieldAuditRevision(item.getFieldAuditRevision())
                .batchItemIndex(item.getBatchItemIndex())
                .fieldPath(item.getFieldPath())
                .fieldKey(item.getFieldKey())
                .fieldLabel(item.getFieldLabel())
                .rowIndex(item.getRowIndex())
                .columnIndex(item.getColumnIndex())
                .component(item.getComponent())
                .valueType(item.getValueType())
                .oldValueJson(item.getOldValueJson())
                .oldValueDisplay(item.getOldValueDisplay())
                .oldValueHash(item.getOldValueHash())
                .newValueJson(item.getNewValueJson())
                .newValueDisplay(item.getNewValueDisplay())
                .newValueHash(item.getNewValueHash())
                .reasonCategory(item.getReasonCategory())
                .reasonText(item.getReasonText())
                .actorId(item.getActorId())
                .actorName(item.getActorName())
                .signatureId(item.getSignatureId())
                .signatureProjectionHash(item.getSignatureProjectionHash())
                .previousHash(item.getPreviousHash())
                .auditHash(item.getAuditHash())
                .beforeCellValuesHash(item.getBeforeCellValuesHash())
                .afterCellValuesHash(item.getAfterCellValuesHash())
                .executionSnapshotHash(item.getExecutionSnapshotHash())
                .changedAt(item.getChangedAt())
                .build();
    }

}
