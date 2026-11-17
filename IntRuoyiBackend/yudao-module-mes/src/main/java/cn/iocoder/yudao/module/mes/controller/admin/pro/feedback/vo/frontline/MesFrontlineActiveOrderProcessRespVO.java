package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - MES 一线生产活跃订单冻结工序 Response VO")
@Data
public class MesFrontlineActiveOrderProcessRespVO {

    private Long activeOrderId;
    private Long routeId;
    private Long routeVersionId;
    private String routeCode;
    private String routeName;
    private Long routeProcessId;
    private Long processId;
    private String processCode;
    private String processName;
    private Integer sort;
    private Long workstationId;
    private String workstationCode;
    private String workstationName;
    private BigDecimal lossCoefficient;
    private BigDecimal targetQuantity;
    private BigDecimal submittedQuantity;
    @Schema(description = "按冻结输出零件统计的累计提交量（含待复核），键为物料ID")
    private Map<Long, BigDecimal> outputSubmittedQuantities;
}
