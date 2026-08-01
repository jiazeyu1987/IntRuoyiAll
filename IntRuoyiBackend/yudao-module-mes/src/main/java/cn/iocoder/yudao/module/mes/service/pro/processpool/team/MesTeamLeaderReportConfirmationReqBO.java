package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderReportConfirmationReqBO {

    private Long eventId;
    private Long leaderUserId;
    private String leaderType;
    private String allocationMode;
    private String reviewRemark;
    private List<MesTeamLeaderReportAllocationLineReqBO> allocations;
}
