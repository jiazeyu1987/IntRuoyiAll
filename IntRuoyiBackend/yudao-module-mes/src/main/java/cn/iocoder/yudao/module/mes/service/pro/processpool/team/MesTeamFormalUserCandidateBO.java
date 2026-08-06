package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesTeamFormalUserCandidateBO {

    private Long systemUserId;
    private String displayName;
    private Boolean disabled;
    private String disabledReason;
    private Boolean occupiedByOtherPqcLeader;
    private Long occupiedLeaderUserId;

    public MesTeamFormalUserCandidateBO(Long systemUserId, String displayName) {
        this.systemUserId = systemUserId;
        this.displayName = displayName;
        this.disabled = Boolean.FALSE;
        this.occupiedByOtherPqcLeader = Boolean.FALSE;
    }
}
