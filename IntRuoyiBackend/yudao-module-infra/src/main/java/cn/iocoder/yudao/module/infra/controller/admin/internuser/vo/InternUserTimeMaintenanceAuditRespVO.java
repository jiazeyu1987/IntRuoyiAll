package cn.iocoder.yudao.module.infra.controller.admin.internuser.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 实习用户时间维护审计 Response VO")
@Data
public class InternUserTimeMaintenanceAuditRespVO {

    @Schema(description = "审计编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "对象类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetType;

    @Schema(description = "对象编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @Schema(description = "对象名称")
    private String targetName;

    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "原时间")
    private LocalDateTime oldTime;

    @Schema(description = "新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime newTime;

    @Schema(description = "操作人")
    private Long operatorUserId;

    @Schema(description = "操作时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
