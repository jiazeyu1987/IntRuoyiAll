package cn.iocoder.yudao.module.mes.dal.dataobject.pro.route;

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

/**
 * MES 工艺流程工序批记录 DO
 */
@TableName("mes_pro_route_flow_process_batch_record")
@KeySequence("mes_pro_route_flow_process_batch_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteFlowProcessBatchRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long routeFlowProcessConfigId;

    private Long routeId;

    private Long routeProcessId;

    private String useType;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String formSlotType;

    private String formBindingKey;

    private String globalSyncKey;

    private Long formTemplateId;

    private String formTemplateNameSnapshot;

    private Long lastPublishedTemplateVersionId;

    private String lastPublishedTemplateVersionNo;

    private String instanceScope;

    private String sharedFormKey;

    private String fillableScopeJson;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private String recordCategorySnapshotHash;

    private String requiredPolicy;

    private String requiredConditionJson;

    private String ownerRoleKey;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private String candidateSourceType;

    private String candidateSourceIds;

    private String candidateSourceNames;

    private Integer reportSort;

    private String remark;

}
