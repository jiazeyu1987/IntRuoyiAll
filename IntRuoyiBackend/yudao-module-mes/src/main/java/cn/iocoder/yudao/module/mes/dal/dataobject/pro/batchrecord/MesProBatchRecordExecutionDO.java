package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_batch_record_execution")
@KeySequence("mes_pro_batch_record_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionDO extends BaseDO {

    @TableId
    private Long id;

    private String executionCode;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private Long workOrderId;

    private String workOrderCode;

    private Long routeProcessId;

    private Long taskId;

    private Long workstationId;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private Long batchExecutionId;

    private Long routeId;

    private String instanceScope;

    private String sharedFormKey;

    private String formSlotType;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private String batchCode;

    private Integer status;

    private String sheetLayoutJson;

    private String metaJson;

    private String executionSnapshotJson;

    private String cellValuesJson;

    private String cellValuesHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private Long fieldAuditLastBatchId;

    private String remark;

    private String activeContextKey;

    private Long revisionRootExecutionId;

    private Integer revisionNo;

    private Long sourceRejectedExecutionId;

    private Long supersededByExecutionId;

    private String revisionReason;

    private String revisionParentHash;

    private Boolean activeRevisionFlag;

    private Long voidedByChangeEventId;

    private Long reopenedByChangeEventId;

    private Long supplementSourceExecutionId;

    private String supplementReason;

    private Boolean supplementFlag;

    private Long effectiveReplacedByExecutionId;

    private String processDefinitionKey;

    private String processInstanceId;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private LocalDateTime closedAt;

    private Long domainTraceSnapshotId;

    private String domainTraceHash;

    private String domainTraceStatus;

    private LocalDateTime domainTraceVerifiedAt;
}
