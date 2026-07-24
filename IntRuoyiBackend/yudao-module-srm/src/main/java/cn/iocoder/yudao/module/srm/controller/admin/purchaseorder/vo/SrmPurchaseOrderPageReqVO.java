package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM 采购订单协同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmPurchaseOrderPageReqVO extends PageParam {

    @Schema(description = "订单编号", example = "PO-20260621-0001")
    private String orderNo;

    @Schema(description = "来源采购计划编号", example = "PL-20260621-0001")
    private String sourcePlanNo;

    @Schema(description = "供应商名称", example = "SRM Portal E2E")
    private String supplierName;

    @Schema(description = "订单状态", example = "PENDING_CONFIRM")
    private String orderStatus;
}
