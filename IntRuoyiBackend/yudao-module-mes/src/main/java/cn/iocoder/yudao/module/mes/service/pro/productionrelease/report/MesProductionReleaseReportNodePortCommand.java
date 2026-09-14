package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportNodePortCommand {

    private Long applicationId;
    private Long actorUserId;
    private Long workTaskId;
    private Long batchExecutionId;
    private Long batchTaskId;
    private String nodeType;
    private String sterilizationBatchNo;
    private List<MesProEdhrSpecialNodeAttachment> attachments;
}
