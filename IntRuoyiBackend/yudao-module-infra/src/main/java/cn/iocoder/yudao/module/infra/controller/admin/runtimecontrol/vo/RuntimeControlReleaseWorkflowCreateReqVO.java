package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 创建程序发布工作流 Request VO")
@Data
public class RuntimeControlReleaseWorkflowCreateReqVO {

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作原因不能为空")
    private String reason;

    @Schema(description = "服务端批准的源码选择编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "源码选择编号不能为空")
    private String sourceSelectionId;

    @JsonAnySetter
    public void rejectClientOwnedField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED: " + fieldName);
    }
}
