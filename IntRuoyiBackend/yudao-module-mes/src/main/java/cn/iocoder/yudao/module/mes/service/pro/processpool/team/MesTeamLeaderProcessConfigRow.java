package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderProcessConfigRow {

    private Long routeId;
    private String routeCode;
    private String routeName;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private Integer sort;
    private List<MesTeamLeaderLossReasonItem> lossReasons;
    private List<MesTeamLeaderProcessConfigDevice> devices;
}
