package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrStage4DossierUploadSimulationReqVO {

    @NotBlank(message = "simulationRunId 不能为空")
    private String simulationRunId;

    @jakarta.validation.constraints.NotNull(message = "batchExecutionId 不能为空")
    private Long batchExecutionId;

    @NotBlank(message = "stage2_5SimulationRunId 不能为空")
    private String stage2_5SimulationRunId;
}
