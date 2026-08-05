package cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "管理后台 - ERP NAS 表格同步计划保存 Request VO")
@Data
public class ErpNasTableSyncPlanSaveReqVO {

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @Schema(description = "每日开始时间", example = "02:30:00")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime dailyStartTime;

    @Schema(description = "NAS 相对目录", example = "ERP/自动同步")
    private String nasDirectory;

    @Schema(description = "文件名规则", example = "ERP_NAS_TABLE_SYNC_{yyyyMMdd_HHmmss}.xlsx")
    private String fileNamePattern;

    @Schema(description = "计划明细")
    @Valid
    private List<ErpNasTableSyncPlanItemSaveReqVO> items;
}
