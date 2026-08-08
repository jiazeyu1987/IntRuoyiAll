package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseDossierSummary {

    private Integer batchRecordCount;
    private Integer processInspectionFormCount;
    private Integer lossReportFormCount;
    private Integer signatureEvidenceCount;
    private String sourceSnapshotHash;
}
