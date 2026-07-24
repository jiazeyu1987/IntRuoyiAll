package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 报表目录分页 Request VO")
@Data
public class MesProEdhrReportCatalogPageReqVO extends PageParam {

    @Schema(description = "报表编码", example = "PRODUCTION_TRACE")
    private String reportCode;

    @Schema(description = "报表名称", example = "生产追溯")
    private String reportName;

    @Schema(description = "报表分类", example = "生产")
    private String reportCategory;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    @Schema(description = "验收状态", example = "FIRST_SLICE_READY")
    private String acceptanceStatus;
}
