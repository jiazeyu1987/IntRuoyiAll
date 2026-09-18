package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesPqcReleaseDossierPlan {

    private String sourceSnapshotHash;
    private Boolean completionBackfillDossier;
    private MesFlow6CompletionBackfillReceipt completionBackfillReceipt;
    private MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchRecordPlan;
    private MesTeamLeaderActiveOrderReleaseProcessInspectionPlan processInspectionPlan;
    private MesTeamLeaderActiveOrderReleaseLossReportPlan lossReportPlan;
}
