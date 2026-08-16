package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
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

@TableName("mes_pro_edhr_work_task")
@KeySequence("mes_pro_edhr_work_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrWorkTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskCode;

    private String taskType;

    private Long batchExecutionId;

    private Long batchTaskId;

    private String businessScopeType;

    private Long businessScopeId;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long pqcReleaseApplicationScopeId;

    private Long executionId;

    private Long sourceExecutionId;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Long routeId;

    private Long routeProcessId;

    private Long processId;

    private String processName;

    private Long assigneeUserId;

    private String candidateSourceType;

    private Long candidateSourceId;

    private String candidateUserSnapshot;

    private Long sourceUserId;

    private String responsibilitySourceType;

    private String responsibilitySourceKey;

    private String responsibilitySourceVersion;

    private String responsibilitySourceDigest;

    private String responsibilityScopeJson;

    private Boolean ownershipLocked;

    private LocalDateTime ownershipLastTransferredAt;

    private Long ownershipLastTransferredBy;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String bpmTaskId;

    private String status;

    private LocalDateTime dueTime;

    private LocalDateTime overdueAt;

    private String overdueReason;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String actionUrl;

    private String reason;

    private String remark;
}
