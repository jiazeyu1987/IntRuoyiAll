package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - ERP 金蝶全量同步请求 VO")
@Data
public class ErpKingdeeFullSyncReqVO {

    @Schema(description = "同步类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION_ORDER")
    @NotBlank(message = "同步类型不能为空")
    private String syncType;

}