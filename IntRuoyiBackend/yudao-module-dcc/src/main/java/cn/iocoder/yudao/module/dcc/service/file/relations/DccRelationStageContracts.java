package cn.iocoder.yudao.module.dcc.service.file.relations;

import java.time.LocalDateTime;
import java.util.List;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccRelationStageMemberDO;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;

public final class DccRelationStageContracts {
    private DccRelationStageContracts() {}
    /** Supplied only by W05 after validating the real BPM task and department obligation. */
    public record ValidatedTaskIdentity(Long tenantId, String processInstanceId, String taskId,
            String obligationId, String stageCode, String nodeInstanceId, Long departmentId,
            String actionType,String applicationType,Integer attributeRound,Long formActionInstanceId) {}
    public record Selection(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long relatedMasterId,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long selectedControlledFileId,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long projectId,
            String fileNumber, String fileName, String versionNo, String origin, boolean supplemental) {}
    public record Prepared(Long actorId, Long sourceFileId, ValidatedTaskIdentity task,
            String commandKey, String expectedDigest, String payloadHash, String completeDigest,
            String reason, List<Selection> complete, List<Arrangement> arrangements) {
        public Prepared { complete=List.copyOf(complete); arrangements=List.copyOf(arrangements); }
        public String signedReasonMarker() { return "dccRelationPayloadHash=" + payloadHash; }
    }
    /** Identity must be taken from the real DCC signature projection returned by the signing service. */
    public record SignatureIdentity(Long signatureId, LocalDateTime signedAt, String evidenceHash) {}
    public record Saved(DccRelationStageSnapshotDO header, List<DccRelationStageMemberDO> members) {
        public Saved { members=List.copyOf(members); }
    }
}
