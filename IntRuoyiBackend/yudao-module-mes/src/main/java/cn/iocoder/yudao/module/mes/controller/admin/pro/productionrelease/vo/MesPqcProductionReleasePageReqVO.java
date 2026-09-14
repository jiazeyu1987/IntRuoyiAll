package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - MES PQC 生产放行分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPqcProductionReleasePageReqVO extends PageParam {

    @Schema(description = "视图状态：PENDING/RELEASED/VOIDED/REWORKED/CONCESSION_RELEASED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "视图状态不能为空")
    @Pattern(regexp = "PENDING|RELEASED|VOIDED|REWORKED|CONCESSION_RELEASED", message = "视图状态不正确")
    private String viewStatus;

    @Schema(description = "工单号")
    private String workOrderCode;

    @Schema(description = "批次号")
    private String batchCode;
}
