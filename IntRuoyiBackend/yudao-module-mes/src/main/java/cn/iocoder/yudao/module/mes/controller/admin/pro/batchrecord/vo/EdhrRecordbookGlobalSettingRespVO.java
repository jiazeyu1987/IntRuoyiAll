package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 记录本全局开关 Response VO")
@Data
@Accessors(chain = true)
public class EdhrRecordbookGlobalSettingRespVO {

    @Schema(description = "是否启用记录本")
    private Boolean enabled;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "最后更新人")
    private String updatedBy;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
