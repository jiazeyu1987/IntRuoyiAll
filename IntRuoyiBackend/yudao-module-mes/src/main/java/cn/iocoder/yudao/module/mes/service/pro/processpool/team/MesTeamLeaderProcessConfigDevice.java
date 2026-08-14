package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderProcessConfigDevice {

    private Long bindingId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceStatus;
    private Boolean mapped;
    private List<MesTeamLeaderProcessConfigParameter> parameters;
}
