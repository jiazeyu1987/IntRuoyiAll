package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工艺路线 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProRouteRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "工艺路线编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ROUTE001")
    @ExcelProperty("工艺路线编码")
    private String code;

    @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "标准路线")
    @ExcelProperty("工艺路线名称")
    private String name;

    @Schema(description = "工艺路线说明")
    @ExcelProperty("工艺路线说明")
    private String description;

    @Schema(description = "负责人", example = "张三")
    private String ownerName;

    @Schema(description = "关键工序", example = "绕簧")
    private String keyProcessName;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "末道工序", example = "末道工序A")
    private String lastProcessName;

    @Schema(description = "关联产品编号", example = "PRD-TAB-STD-001、PRD-TAB-PKG-002")
    private String productCodes;

    @Schema(description = "关系图是否已设置", example = "true")
    private Boolean flowGraphConfigured;

    @Schema(description = "当前激活路线版本编号", example = "100")
    private Long activeRouteVersionId;

    @Schema(description = "当前激活路线版本号", example = "V2")
    private String activeRouteVersionNo;

    @Schema(description = "待发布路线版本编号", example = "101")
    private Long pendingRouteVersionId;

    @Schema(description = "待发布路线版本号", example = "V3")
    private String pendingRouteVersionNo;

    @Schema(description = "待发布路线版本状态", example = "READY_TO_PUBLISH")
    private String pendingRouteVersionStatus;

    @Schema(description = "待发布候选版本数量", example = "2")
    private Integer pendingRouteVersionCount;

    @Schema(description = "工艺流程排产配置用途是否启用", example = "true")
    private Boolean scheduleRouteEnabled;

    @Schema(description = "工艺流程批记录配置用途是否启用", example = "false")
    private Boolean batchRouteEnabled;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
