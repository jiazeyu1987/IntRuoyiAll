package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseManagerStageInitializationCommand {

    private Long applicationId;
    private Long batchExecutionId;
    private String reportSnapshotHash;
    private List<MesProductionReleaseReportNodeEvidence> reportEvidences;
    private Integer expectedApplicationVersion;
}
