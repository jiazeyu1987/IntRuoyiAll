package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理后台 - IDI Stage6 模拟数据生成 Request VO")
public class MesStage6IdiSimulationReqVO {

    @NotBlank(message = "simulationRunId 不能为空")
    private String simulationRunId;

}
