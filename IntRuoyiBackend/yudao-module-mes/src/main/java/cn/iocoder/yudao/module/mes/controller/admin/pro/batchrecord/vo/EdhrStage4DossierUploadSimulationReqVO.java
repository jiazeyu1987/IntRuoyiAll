package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrStage4DossierUploadSimulationReqVO {

    @NotBlank(message = "inputMode 不能为空")
    private String inputMode;

    @NotBlank(message = "simulationRunId 不能为空")
    private String simulationRunId;

    private Long batchExecutionId;

    private String stage2_5SimulationRunId;
}
