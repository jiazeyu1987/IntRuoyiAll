package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 金蝶同步水位 Response VO")
@Data
public class ErpKingdeeSyncWatermarkRespVO {

    @Schema(description = "同步类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String syncType;

    @Schema(description = "最近成功水位")
    private LocalDateTime lastSuccessTime;
}
