package cn.iocoder.yudao.module.dcc.service.file.relations;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Saved;

/** Historical snapshots remain visible with their independent signature state. */
public record DccRelationRoundEvidence(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long fileId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long masterId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
        String applicationType,Integer attributeRound,String bpmRound,boolean recorded,String unavailableReason,
        List<Selection> originalUploadSelections,List<StageEvidence> stages,List<Selection> completeSelections,String relationDigest) {
    public DccRelationRoundEvidence {
        originalUploadSelections=List.copyOf(originalUploadSelections);stages=List.copyOf(stages);completeSelections=List.copyOf(completeSelections);
    }
    public record StageEvidence(Saved snapshot,String signatureEvidenceStatus,boolean signatureIdentityMatches) {}
}
