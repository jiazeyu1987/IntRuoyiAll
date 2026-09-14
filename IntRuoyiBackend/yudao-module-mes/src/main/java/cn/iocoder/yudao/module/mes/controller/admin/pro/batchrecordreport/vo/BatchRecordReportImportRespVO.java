package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 电子批记录导入生成报表 Response VO")
@Data
public class BatchRecordReportImportRespVO {

    @Schema(description = "导入生成报表总数", example = "10")
    private Integer importedCount;

    @Schema(description = "新建报表数", example = "10")
    private Integer createdCount;

    @Schema(description = "覆盖更新报表数", example = "0")
    private Integer updatedCount;

    @Schema(description = "批记录定义 ID", example = "1001")
    private Long batchRecordDefinitionId;

    @Schema(description = "本次导入生成或复用的批记录版本 ID", example = "2001")
    private Long batchRecordVersionId;

    @Schema(description = "来源批记录版本 ID", example = "1000")
    private Long sourceBatchRecordVersionId;

    @Schema(description = "本次导入生成或复用的批记录版本号", example = "V2.0")
    private String versionNo;

    @Schema(description = "本次导入生成或复用的批记录版本状态", example = "PRECHECK_PASSED")
    private String versionStatus;

    @Schema(description = "升版导入自动提交后生成的审批实例 ID", example = "BRV-2001-1")
    private String approvalInstanceId;

    @Schema(description = "本次自动生成的工艺路线 ID", example = "1001")
    private Long routeId;

    @Schema(description = "本次自动生成的工艺路线编码", example = "ROUTE202607070001")
    private String routeCode;

    @Schema(description = "本次自动生成的工艺路线名称", example = "球囊扩张压力泵方案")
    private String routeName;

    @Schema(description = "本次自动生成或维护的工艺路线版本 ID", example = "2001")
    private Long routeVersionId;

    @Schema(description = "本次自动生成或维护的工艺路线版本号", example = "V1")
    private String routeVersionNo;

    @Schema(description = "本次自动生成的路线工序数", example = "14")
    private Integer routeProcessCount;

    @Schema(description = "本次自动生成的工艺流程批记录配置绑定数", example = "14")
    private Integer batchRecordRouteBindingCount;

    @Schema(description = "本次绑定到工艺路线的产品名称数", example = "2")
    private Integer boundProductNameCount;

    @Schema(description = "本次绑定到工艺路线的产品编码数", example = "4")
    private Integer boundProductCodeCount;

    @Schema(description = "因未找到产品编码而跳过的产品名称")
    private List<String> skippedProductNames;

    @Schema(description = "本次处理的报表")
    private List<BatchRecordReportRespVO> reports;

    @Schema(description = "本次 Word 导入识别出的产品、工序、物料、设备和参数总 JSON")
    private String totalRecognitionJson;
}
