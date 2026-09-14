package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportStageInitializationCommand {

    private Long applicationId;
    private Long batchExecutionId;
    private Long routeId;
    private Long routeVersionId;
    private String sourceSnapshotHash;
    private Integer expectedApplicationVersion;
}
