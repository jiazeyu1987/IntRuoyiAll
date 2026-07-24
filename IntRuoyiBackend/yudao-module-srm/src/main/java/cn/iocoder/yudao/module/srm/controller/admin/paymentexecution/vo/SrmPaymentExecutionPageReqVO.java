package cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM 付款执行分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmPaymentExecutionPageReqVO extends PageParam {

    @Schema(description = "付款执行单号", example = "PE-20260621-0001")
    private String paymentNo;

    @Schema(description = "对账单号", example = "OR-20260621-0001")
    private String reconciliationNo;

    @Schema(description = "供应商名称", example = "SRM Phase 5 供应商")
    private String supplierName;

    @Schema(description = "付款状态", example = "APPROVED")
    private String paymentStatus;
}
