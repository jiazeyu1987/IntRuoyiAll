package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class MesProBatchRecordExecutionFieldAuditItemHashInput {

    String fieldPath;

    String fieldKey;

    Integer rowIndex;

    Integer columnIndex;

    MesProBatchRecordExecutionFieldAuditValueType valueType;

    String oldValueJson;

    String oldValueDisplay;

    String oldValueHash;

    String newValueJson;

    String newValueDisplay;

    String newValueHash;

    String reasonCategory;

    String reasonText;

    Long actorId;

    String actorName;

    String signatureProjectionHash;

    String previousHash;

    LocalDateTime changedAt;
}
