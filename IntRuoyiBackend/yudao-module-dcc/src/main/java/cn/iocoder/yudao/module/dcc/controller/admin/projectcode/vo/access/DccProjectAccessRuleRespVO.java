package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目正式权限 Response VO")
@Data
public class DccProjectAccessRuleRespVO {

    @Schema(description = "规则编号")
    private Long id;

    @Schema(description = "DCC 项目代码编号")
    private Long dccProjectCodeId;

    @Schema(description = "授权主体类型：USER/DEPT/ROLE/POSITION")
    private String subjectType;

    @Schema(description = "授权主体编号")
    private Long subjectId;

    @Schema(description = "访问级别：OWNER/EDIT/VIEW")
    private String accessLevel;

    @Schema(description = "是否启用")
    private Boolean active;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "变更原因")
    private String changeReason;
}
