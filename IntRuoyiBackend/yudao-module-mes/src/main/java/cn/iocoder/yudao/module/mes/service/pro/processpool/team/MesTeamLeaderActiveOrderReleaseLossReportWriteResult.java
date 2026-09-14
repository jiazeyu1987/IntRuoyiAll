package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseLossReportWriteResult {

    private String documentType;
    private List<Long> batchRecordExecutionIds;
    private List<Long> formCenterInstanceIds;
    private List<Long> fieldAuditIds;
    private List<String> fieldAuditHeadHashes;
    private List<Long> sourceObjectIds;
    private List<String> sourceValueHashes;
    private List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatureEvidence;
    private List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documentEvidence;
    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;
    private String lossReportStatus;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String lossDecision;
    private List<MesTeamLeaderActiveOrderReleaseLossReportPlan.ProcessLossDecision> processDecisions;
    private String sourceSnapshotHash;
}
