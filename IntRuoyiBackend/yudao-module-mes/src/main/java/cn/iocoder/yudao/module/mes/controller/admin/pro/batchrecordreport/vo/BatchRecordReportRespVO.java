package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 电子批记录生成报表 Response VO")
@Data
public class BatchRecordReportRespVO {

    @Schema(description = "批记录名称")
    private String batchRecordName;

    @Schema(description = "批记录定义 ID")
    private Long batchRecordDefinitionId;

    @Schema(description = "批记录版本 ID")
    private Long batchRecordVersionId;

    @Schema(description = "表单对应的产品名称")
    private String productName;

    @Schema(description = "DCC 项目代码")
    private String projectCode;

    @Schema(description = "批记录版本号")
    private String versionNo;

    @Schema(description = "批记录版本状态")
    private String versionStatus;

    @Schema(description = "表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD")
    private String formSlotType;

    @Schema(description = "识别路线关键字")
    private String routeKey;

    @Schema(description = "来源表序号", example = "1")
    private Integer sourceTableIndex;

    @Schema(description = "来源表标题")
    private String tableTitle;

    @Schema(description = "积木报表 ID")
    private String reportId;

    @Schema(description = "积木报表编码")
    private String reportCode;

    @Schema(description = "积木报表名称")
    private String reportName;

    @Schema(description = "来源文件名")
    private String sourceFileName;

    @Schema(description = "最近导入时间")
    private LocalDateTime lastImportTime;

    @Schema(description = "最近修改时间")
    private LocalDateTime updateTime;
}
