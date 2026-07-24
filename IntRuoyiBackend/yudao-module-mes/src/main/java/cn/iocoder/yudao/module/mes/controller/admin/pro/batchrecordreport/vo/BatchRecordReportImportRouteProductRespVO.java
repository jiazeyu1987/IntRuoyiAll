package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 电子批记录 Word 导入产线预检选项 Response VO")
@Data
public class BatchRecordReportImportRouteProductRespVO {

    @Schema(description = "前端选择键", example = "ROUTE_PRODUCT:1001")
    private String optionKey;

    @Schema(description = "工艺路线产品绑定 ID", example = "1001")
    private Long routeProductId;

    @Schema(description = "工艺路线 ID", example = "2001")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "ROUTE202607120001")
    private String routeCode;

    @Schema(description = "工艺路线名称", example = "球囊扩张压力泵方案")
    private String routeName;

    @Schema(description = "当前工艺路线版本 ID", example = "3001")
    private Long routeVersionId;

    @Schema(description = "当前工艺路线版本号", example = "V1")
    private String routeVersionNo;

    @Schema(description = "产品物料 ID", example = "4001")
    private Long productId;

    @Schema(description = "产品物料编码", example = "BRP-001")
    private String productCode;

    @Schema(description = "产品名称", example = "球囊扩张压力泵")
    private String productName;

    @Schema(description = "是否已有工艺路线产品绑定", example = "true")
    private Boolean existing;
}
