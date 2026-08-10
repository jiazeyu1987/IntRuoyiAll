package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult {

    private String documentType;

    private List<Long> batchRecordExecutionIds;

    private List<Long> fieldAuditIds;

    private List<String> fieldAuditHeadHashes;

    private List<Long> sourceObjectIds;

    private List<String> sourceValueHashes;

    private List<SignatureEvidence> signatureEvidence;

    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;

    @Data
    @Accessors(chain = true)
    public static class SignatureEvidence {

        private String role;

        private String sourceType;

        private Long sourceId;

        private Long signatureId;

        private Long userId;

        private LocalDateTime signedAt;

        private String evidenceHash;
    }
}
