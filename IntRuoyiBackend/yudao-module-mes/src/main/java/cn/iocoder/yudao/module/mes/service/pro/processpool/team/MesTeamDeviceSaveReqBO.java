package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamDeviceSaveReqBO {

    private Long leaderUserId;
    private String deviceCode;
    private String deviceName;
    private String deviceStatus;
}
