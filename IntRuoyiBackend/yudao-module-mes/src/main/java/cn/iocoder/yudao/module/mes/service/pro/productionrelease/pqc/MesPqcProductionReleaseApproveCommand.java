package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleaseApproveCommand {

    private Long applicationId;
    private Long pqcReleaseWorkTaskId;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String approvalOpinion;
}
