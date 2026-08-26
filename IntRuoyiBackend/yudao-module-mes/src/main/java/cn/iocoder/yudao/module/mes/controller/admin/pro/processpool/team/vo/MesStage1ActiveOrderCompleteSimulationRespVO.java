package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Schema(description = "管理后台 - MES Stage1 活跃订单完成模拟 Response VO")
@Data
@Accessors(chain = true)
public class MesStage1ActiveOrderCompleteSimulationRespVO {

    private String simulationRunId;
    private String cleanedSimulationRunId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long pickListId;
    private Integer productionSubmitCount;
    private Integer productionReviewCount;
    private Integer pqcSubmitCount;
    private Integer pqcReviewCount;
    private Boolean productionProgress100;
    private Boolean inspectionProgress100;
    private Boolean completionButtonEnabled;
    private Map<String, Object> activeOrderCompleteSnapshot;
}
