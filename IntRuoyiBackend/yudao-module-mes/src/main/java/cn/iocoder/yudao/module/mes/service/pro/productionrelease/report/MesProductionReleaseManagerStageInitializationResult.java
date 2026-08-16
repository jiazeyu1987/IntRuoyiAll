package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseManagerStageInitializationResult {

    private Long releaseTransactionId;
    private Long managerReleaseWorkTaskId;
    private String managerCandidateSnapshotHash;
}
