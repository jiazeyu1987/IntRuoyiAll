package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 临时角色授权创建 Request VO")
@Data
public class TemporaryRoleGrantCreateReqVO {

    @Schema(description = "被授权用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "被授权用户不能为空")
    private Long userId;

    @Schema(description = "临时角色编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "临时角色不能为空")
    private Long roleId;

    @Schema(description = "授权原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权原因不能为空")
    private String reason;

    @Schema(description = "有效截止时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效截止时间不能为空")
    @Future(message = "有效截止时间必须晚于当前时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expireTime;

}
