package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionGovernanceInspectionRespVO {

    private Long versionId;

    private String inspectionCode;

    private String inspectionStatus;

    private Long issueCount;

    private String issueSummary;

    private String nextAction;
}
