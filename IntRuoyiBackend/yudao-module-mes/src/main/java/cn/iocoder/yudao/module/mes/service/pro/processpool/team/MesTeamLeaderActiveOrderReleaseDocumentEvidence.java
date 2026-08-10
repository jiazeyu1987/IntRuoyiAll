package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseDocumentEvidence {

    private String documentType;
    private Long batchExecutionId;
    private Long batchExecutionTaskId;
    private List<Long> batchRecordExecutionIds;
    private List<String> targetReportIds;
    private List<Long> targetDefinitionIds;
    private List<Long> targetVersionIds;
    private List<String> targetSnapshotHashes;
    private List<Long> fieldAuditIds;
    private int requiredFieldCount;
    private int auditedRequiredFieldCount;
    private List<Long> sourceObjectIds;
    private List<String> sourceValueHashes;
    private List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatureEvidence;
    private String sourceSnapshotHash;
    private boolean sourceConsistent;
}
