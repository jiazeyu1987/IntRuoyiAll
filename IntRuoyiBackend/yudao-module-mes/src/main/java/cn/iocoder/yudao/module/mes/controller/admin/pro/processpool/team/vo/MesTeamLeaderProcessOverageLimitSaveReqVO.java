package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MesTeamLeaderProcessOverageLimitSaveReqVO {
    @NotNull
    private Long routeProcessId;
    @NotNull
    private Long processId;
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal overagePercent;
}
