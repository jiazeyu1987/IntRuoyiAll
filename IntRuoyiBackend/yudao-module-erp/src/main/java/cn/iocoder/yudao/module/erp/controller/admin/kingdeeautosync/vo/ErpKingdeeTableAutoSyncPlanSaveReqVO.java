package cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 表格自动同步计划保存 Request VO")
@Data
public class ErpKingdeeTableAutoSyncPlanSaveReqVO {

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "每日开始时间")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime dailyStartTime;

    @Schema(description = "计划明细")
    @Valid
    private List<ErpKingdeeTableAutoSyncPlanItemSaveReqVO> items;
}
