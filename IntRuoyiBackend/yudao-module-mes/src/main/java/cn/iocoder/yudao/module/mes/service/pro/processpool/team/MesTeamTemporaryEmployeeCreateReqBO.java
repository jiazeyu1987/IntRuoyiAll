package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamTemporaryEmployeeCreateReqBO {

    private Long leaderUserId;
    private String displayName;
    private String signaturePassword;
}
