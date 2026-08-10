package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseSignatureEvidence {

    private String role;
    private String sourceType;
    private Long sourceId;
    private Long signatureId;
    private Long userId;
    private LocalDateTime signedAt;
    private String evidenceHash;
}
