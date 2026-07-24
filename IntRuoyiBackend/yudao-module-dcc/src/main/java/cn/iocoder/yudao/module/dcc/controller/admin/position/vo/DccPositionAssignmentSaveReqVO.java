package cn.iocoder.yudao.module.dcc.controller.admin.position.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPositionAssignmentSaveReqVO {
    @NotBlank(message = "分配类型不能为空")
    private String assignmentType;
    private Long systemPostId;
    private Long userId;
    @NotNull(message = "是否启用不能为空")
    private Boolean active;
    private String changeReason;
}
