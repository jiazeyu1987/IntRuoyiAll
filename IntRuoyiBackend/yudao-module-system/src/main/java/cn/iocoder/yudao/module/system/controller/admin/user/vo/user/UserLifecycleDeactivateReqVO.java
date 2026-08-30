package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.user.UserLifecycleDocumentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 用户离职/转岗联动停用 Request VO")
@Data
public class UserLifecycleDeactivateReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "单据类型，RESIGNATION-离职单，TRANSFER-转岗单",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "RESIGNATION")
    @NotBlank(message = "单据类型不能为空")
    @InEnum(value = UserLifecycleDocumentTypeEnum.class, message = "单据类型必须是 {value}")
    private String documentType;

    @Schema(description = "离职/转岗单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "LZ-20260815-001")
    @NotBlank(message = "离职/转岗单号不能为空")
    @Size(max = 64, message = "离职/转岗单号不能超过 64 个字符")
    private String documentNo;

    @Schema(description = "离职/转岗单据时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-01T09:00:00")
    @NotNull(message = "离职/转岗单据时间不能为空")
    private LocalDateTime documentTime;

    @Schema(description = "账号停用生效时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-15T18:00:00")
    @NotNull(message = "账号停用生效时间不能为空")
    private LocalDateTime effectiveTime;

}
