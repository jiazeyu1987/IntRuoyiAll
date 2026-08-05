package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 生产组长损耗原因标准列表 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderLossReasonRowRespVO {

    private Long routeId;
    private String routeCode;
    private String routeName;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private Integer sort;
    private List<MesTeamLeaderLossReasonRespVO> reasons;

}
