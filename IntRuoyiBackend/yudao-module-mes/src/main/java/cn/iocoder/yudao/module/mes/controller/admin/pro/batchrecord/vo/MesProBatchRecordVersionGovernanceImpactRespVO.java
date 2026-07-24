package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionGovernanceImpactRespVO {

    private Long versionId;

    private Long executionCount;

    private Long taskCount;

    private Long routeBindingCount;

    private Long permissionRuleCount;

    private List<String> slotConfigSnapshotHashes;

    private List<String> ownerRoleKeys;

    private String riskLevel;
}
