package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import java.util.List;

/** Authorized, immutable application read evidence; never a draft edit or current project defaults. */
public record DccControlledFileApplicationEvidence(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        String versionNo, String applicationType, String bpmRound, Integer attributeRound,
        boolean recorded, String unavailableReason,
        DccProjectAttributes defaultSource, DccProjectAttributes actualAttributes,
        List<DccControlledFileSignatureSummaryRespVO> signatures,
        cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundEvidence relationEvidence,
        List<DccApplicationNodeRecord> nodeRecords) {
    public DccControlledFileApplicationEvidence { signatures=List.copyOf(signatures);nodeRecords=List.copyOf(nodeRecords); }
    /** Historical absence stays explicit; no projection of today's relation set into an old round. */
    public DccControlledFileApplicationEvidence(Long id,String version,String type,String bpm,Integer attributeRound,
            boolean recorded,String reason,DccProjectAttributes defaults,DccProjectAttributes actual,
            List<DccControlledFileSignatureSummaryRespVO> signatures) {
        this(id,version,type,bpm,attributeRound,recorded,reason,defaults,actual,signatures,null,List.of());
    }
}
