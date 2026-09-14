package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamEmployeeProfileSaveReqBO {

    private Long leaderUserId;
    private Long systemUserId;
    private String employeeCode;
    private String employeeName;
    private String employeeType;
}
