package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目正式权限保存 Request VO")
@Data
public class DccProjectAccessRuleSaveReqVO {

    @Schema(description = "授权主体类型：USER/DEPT/ROLE/POSITION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权主体类型不能为空")
    private String subjectType;

    @Schema(description = "授权主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "授权主体编号不能为空")
    private Long subjectId;

    @Schema(description = "访问级别：OWNER/EDIT/VIEW", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "访问级别不能为空")
    private String accessLevel;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "正式权限规则启用状态不能为空")
    private Boolean active;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更原因不能为空")
    private String changeReason;
}
