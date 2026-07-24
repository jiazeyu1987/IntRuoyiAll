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

@TableName("mes_pro_edhr_operation_audit_event")
@KeySequence("mes_pro_edhr_operation_audit_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrOperationAuditEventDO extends BaseDO {

    @TableId
    private Long id;

    private String requestId;

    private String objectType;

    private String objectId;

    private Long batchExecutionId;

    private Long executionId;

    private Long workTaskId;

    private Long routeId;

    private Long routeProcessId;

    private String reportId;

    private String recordCategory;

    private String operationType;

    private String actionName;

    private Long actorUserId;

    private String actorUsername;

    private String permissionCode;

    private String permissionDecision;

    private String matchedRuleIds;

    private String resultStatus;

    private String failureCode;

    private String failureMessage;

    private String beforeSummaryHash;

    private String afterSummaryHash;

    private String metadataJson;

    private LocalDateTime occurredAt;

    private String previousAuditHash;

    private String auditHash;
}
