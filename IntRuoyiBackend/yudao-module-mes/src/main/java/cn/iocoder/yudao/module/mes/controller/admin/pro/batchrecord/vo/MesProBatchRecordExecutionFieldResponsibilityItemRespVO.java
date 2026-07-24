package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityValueOrigin;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityItemRespVO {

    private String fieldPath;
    private String fieldKey;
    private String fieldLabel;
    private Integer rowIndex;
    private Integer columnIndex;
    private String component;
    private String valueType;
    private String currentValueJson;
    private String currentValueDisplay;
    private String currentValueHash;
    private MesProBatchRecordExecutionResponsibilityValueOrigin valueOrigin;
    private Long firstHumanActorId;
    private String firstHumanActorName;
    private LocalDateTime firstHumanChangedAt;
    private Long currentValueActorId;
    private String currentValueActorName;
    private LocalDateTime currentValueChangedAt;
    private MesProBatchRecordExecutionResponsibilityEvidenceStatus evidenceStatus;
    private List<MesProBatchRecordExecutionResponsibilityReasonCode> reasonCodes;
    private Long historyCount;
    private Long latestAuditItemId;
}
