package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EdhrStage5FinalReleaseSimulationReqVO {

    @NotBlank(message = "simulationRunId 不能为空")
    private String simulationRunId;

    private String previousSimulationRunId;
}
