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

@TableName("mes_pro_edhr_batch_execution_task")
@KeySequence("mes_pro_edhr_batch_execution_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionTaskDO extends BaseDO {

    @TableId
    private Long id;

    private Long batchExecutionId;

    private String nodeType;

    private Long routeProcessId;

    private Long predecessorRouteProcessId;

    private Boolean rootProcessFlag;

    private Integer routeProcessSort;

    private Long processId;

    private String processCode;

    private String processName;

    private String batchRecordReportId;

    private String batchRecordReportName;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private Integer batchRecordSort;

    private String instanceScope;

    private String sharedFormKey;

    private String fillableScopeJson;

    private String executionMode;

    private String formSlotType;

    private String formBindingKey;

    private Long formTemplateId;

    private String formTemplateNameSnapshot;

    private Long formTemplateVersionId;

    private String formTemplateVersionNo;

    private Long formCenterInstanceId;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    /**
     * The Flow7/Flow6 formal source snapshot witnessed when a mandatory
     * release-material task was created. It is intentionally distinct from
     * routeBindingSnapshotHash, which only describes route/form configuration.
     */
    private String materialSourceSnapshotHash;

    private String requiredPolicy;

    private String requiredConditionJson;

    private String ownerRoleKey;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private Long executionId;

    private Integer status;

    private Boolean requiredFlag;

    private String blockerCode;

    private String blockerMessage;

    private Long openedBy;

    private LocalDateTime openedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private Long skippedBy;

    private LocalDateTime skippedAt;

    private String specialPayloadJson;
}
