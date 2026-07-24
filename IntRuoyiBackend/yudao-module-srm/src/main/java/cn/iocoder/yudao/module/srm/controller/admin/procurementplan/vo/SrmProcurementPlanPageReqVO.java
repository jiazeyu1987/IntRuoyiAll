package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - SRM 采购计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmProcurementPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "PL-20260619-0001")
    private String planNo;

    @Schema(description = "计划标题", example = "耗材")
    private String planTitle;

    @Schema(description = "计划状态", example = "APPROVED")
    private String planStatus;
}
