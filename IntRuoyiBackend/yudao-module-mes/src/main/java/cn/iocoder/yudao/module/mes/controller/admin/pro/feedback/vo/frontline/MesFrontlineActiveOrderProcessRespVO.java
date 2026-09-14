package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

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
    private BigDecimal productionQuantityFactor;
    private BigDecimal targetQuantity;
}
