package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM 委外执行分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmOutsourceExecutionPageReqVO extends PageParam {

    @Schema(description = "委外执行单号", example = "OE-20260621-0001")
    private String executionNo;

    @Schema(description = "采购订单协同单号", example = "PO-20260621-0001")
    private String purchaseOrderNo;

    @Schema(description = "供应商名称", example = "SRM Phase 4 供应商")
    private String supplierName;

    @Schema(description = "执行状态", example = "IN_PRODUCTION")
    private String executionStatus;
}
