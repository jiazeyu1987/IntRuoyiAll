package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachment;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportNodeCompleteCommand {

    private Long batchTaskId;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String sterilizationBatchNo;
    private List<MesProEdhrSpecialNodeAttachment> attachments;
}
