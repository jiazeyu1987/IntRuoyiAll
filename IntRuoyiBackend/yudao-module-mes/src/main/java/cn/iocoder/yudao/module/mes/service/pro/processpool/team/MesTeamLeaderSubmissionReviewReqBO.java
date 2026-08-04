package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderSubmissionReviewReqBO {

    private Long eventId;
    private Long leaderUserId;
    private String leaderType;
    private String reviewStatus;
    private String reviewRemark;
    private Long reviewSignatureId;
    private Long reviewSignatureUserId;
    private String reviewSignatureSnapshotJson;
}
