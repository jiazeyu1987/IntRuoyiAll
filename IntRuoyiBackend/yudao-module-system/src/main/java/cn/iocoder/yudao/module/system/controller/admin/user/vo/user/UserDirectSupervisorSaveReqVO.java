package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDirectSupervisorSaveReqVO {
    @NotNull @Positive private Long userId;
    @NotNull @Min(0) private Long expectedRelationVersion;
    @NotNull @Pattern(regexp = "SET|CLEAR") private String operation;
    @Positive private Long supervisorUserId;
    @NotBlank @Size(max = 500) private String changeReason;
}
