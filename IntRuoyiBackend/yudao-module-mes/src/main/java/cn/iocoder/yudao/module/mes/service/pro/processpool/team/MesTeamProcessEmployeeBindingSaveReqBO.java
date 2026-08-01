package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamProcessEmployeeBindingSaveReqBO {

    private Long leaderUserId;
    private Long processId;
    private Long employeeProfileId;
}
